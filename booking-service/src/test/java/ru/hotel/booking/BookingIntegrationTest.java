package ru.hotel.booking;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.hotel.booking.api.BookingDtos.*;
import ru.hotel.booking.api.UserDtos.*;
import ru.hotel.booking.client.*;
import ru.hotel.booking.domain.*;
import ru.hotel.booking.persistence.repository.*;
import ru.hotel.booking.service.*;
import ru.hotel.common.*;
import ru.hotel.common.security.JwtSupport;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

@SpringBootTest(
        properties = {
            "eureka.client.enabled=false",
            "spring.cloud.discovery.enabled=false",
            "spring.datasource.url=jdbc:h2:mem:booking-test;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000"
        })
@AutoConfigureMockMvc
class BookingIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired BookingService service;
    @Autowired BookingTransactions transactions;
    @Autowired BookingJpaRepository bookings;
    @Autowired UserJpaRepository users;
    @Autowired UserService userService;
    @Autowired JwtEncoder encoder;
    @MockitoBean HotelPort hotel;
    private Long alice;
    private Long bob;
    private String token;
    private final LocalDate start = LocalDate.now().plusDays(10);
    private final LocalDate end = start.plusDays(3);

    @BeforeEach
    void setUp() {
        bookings.deleteAll();
        users.deleteAll();
        reset(hotel);
        alice = userService.create(new UserInput("alice", "Alice123!", Role.USER)).id();
        bob = userService.create(new UserInput("bob", "Bob12345!", Role.USER)).id();
        token = JwtSupport.token(encoder, alice.toString(), "USER", 3600);
    }

    @Test
    void successfulSagaCommitsPendingBeforeCallingHotel() {
        doAnswer(
                        invocation -> {
                            ReservationCommand command = invocation.getArgument(1);
                            assertThat(transactions.internal(command.bookingId()).status())
                                    .isEqualTo(BookingStatus.PENDING);
                            return null;
                        })
                .when(hotel)
                .confirm(anyLong(), any());
        var result = service.create(alice, manual());
        assertThat(result.booking().status()).isEqualTo(BookingStatus.CONFIRMED);
        verify(hotel, times(1)).confirm(eq(1L), any());
    }

    @Test
    void conflictCancelsAndCompensates() {
        doThrow(remote(409)).when(hotel).confirm(anyLong(), any());
        var b = service.create(alice, manual()).booking();
        assertThat(b.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(b.failureStatus()).isEqualTo(409);
        assertThat(b.compensationRequired()).isFalse();
        verify(hotel).release(eq(1L), argThat(c -> c.bookingId().equals(b.id())));
    }

    @Test
    void timeoutCancelsAndFailedCompensationRemainsRetryable() {
        doThrow(remote(503)).when(hotel).confirm(anyLong(), any());
        doThrow(remote(503)).doNothing().when(hotel).release(anyLong(), any());
        var b = service.create(alice, manual()).booking();
        assertThat(b.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(b.compensationRequired()).isTrue();
        service.cancel(alice, b.id());
        assertThat(transactions.internal(b.id()).compensationRequired()).isFalse();
        verify(hotel, times(2)).release(eq(1L), any());
    }

    @Test
    void autoSelectionSortsAndTriesNextRoomOnConflict() {
        when(hotel.recommend(any(), any(), any()))
                .thenReturn(
                        List.of(
                                new RoomView(2L, 1L, "102", true, 0),
                                new RoomView(1L, 1L, "101", true, 0),
                                new RoomView(3L, 1L, "103", true, 8)));
        doThrow(remote(409)).when(hotel).confirm(eq(1L), any());
        var b =
                service.create(
                                alice,
                                new CreateBooking(UUID.randomUUID(), true, 999L, 1L, start, end))
                        .booking();
        assertThat(b.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(b.roomId()).isEqualTo(2L);
        var order = inOrder(hotel);
        order.verify(hotel).recommend(1L, start, end);
        order.verify(hotel).confirm(eq(1L), any());
        order.verify(hotel).confirm(eq(2L), any());
    }

    @Test
    void repeatedCancellationReleasesSlotOnce() {
        var b = service.create(alice, manual()).booking();
        service.cancel(alice, b.id());
        service.cancel(alice, b.id());
        assertThat(transactions.internal(b.id()).status()).isEqualTo(BookingStatus.CANCELLED);
        verify(hotel, times(1)).release(anyLong(), any());
    }

    @Test
    void userSeesOnlyOwnBookingsAndHistoryIsPaginated() throws Exception {
        var own = service.create(alice, manual()).booking();
        var other = service.create(bob, manual()).booking();
        mvc.perform(
                        get("/api/bookings/{id}", other.id())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mvc.perform(
                        delete("/api/bookings/{id}", other.id())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mvc.perform(
                        get("/api/bookings")
                                .header("Authorization", "Bearer " + token)
                                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(own.id().toString()));
    }

    @Test
    void authRegistrationCannotCreateAdministratorAndPasswordIsHashed() throws Exception {
        mvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\"newuser\",\"password\":\"Password123!\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.token").isString());
        assertThat(users.findByUsername("newuser").orElseThrow().getPassword()).startsWith("$2a$");
        mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"newuser\",\"password\":\"Wrong123!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticationAndValidationHaveCorrectStatusCodes() throws Exception {
        mvc.perform(get("/api/bookings")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/bookings")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                new CreateBooking(
                                                        UUID.randomUUID(),
                                                        false,
                                                        1L,
                                                        null,
                                                        end,
                                                        start))))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/bookings")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                new CreateBooking(
                                                        UUID.randomUUID(),
                                                        false,
                                                        1L,
                                                        null,
                                                        start.minusDays(20),
                                                        end))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void httpCreationAndReplayReturnSameBooking() throws Exception {
        String body = json.writeValueAsString(manual());
        String first =
                mvc.perform(
                                post("/api/bookings")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("CONFIRMED"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        mvc.perform(
                        post("/api/bookings")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(json.readTree(first).get("id").asText()));
    }

    private CreateBooking manual() {
        return new CreateBooking(UUID.randomUUID(), false, 1L, null, start, end);
    }

    @Test
    void autoSelectionIgnoresRoomIdEvenWhenItIsNegative() throws Exception {
        when(hotel.recommend(any(), any(), any()))
                .thenReturn(List.of(new RoomView(2L, 1L, "102", true, 0)));
        var request = new CreateBooking(UUID.randomUUID(), true, -1L, 1L, start, end);
        mvc.perform(
                        post("/api/bookings")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(2));
    }

    @Test
    void over72BytePasswordsReturn400InsteadOf500() throws Exception {
        var payload = json.writeValueAsString(new Credentials("newuser", "я".repeat(40)));
        mvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                new Credentials("alice", "я".repeat(40)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCannotSetBlankPassword() throws Exception {
        var adminToken = JwtSupport.token(encoder, alice.toString(), "ADMIN", 3600);
        mvc.perform(
                        patch("/api/users/{id}", bob)
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"password\":\"        \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void manualSelectionRequiresPositiveRoomId() throws Exception {
        var request = new CreateBooking(UUID.randomUUID(), false, -1L, null, start, end);
        mvc.perform(
                        post("/api/bookings")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        verify(hotel, never()).confirm(anyLong(), any());
    }

    @Test
    void failedBookingUsesErrorDtoAndKeepsCancelledBookingAccessible() throws Exception {
        doThrow(remote(503)).when(hotel).confirm(anyLong(), any());
        String result =
                mvc.perform(
                                post("/api/bookings")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json.writeValueAsString(manual())))
                        .andExpect(status().isServiceUnavailable())
                        .andExpect(jsonPath("$.status").value(503))
                        .andExpect(jsonPath("$.bookingId").isString())
                        .andExpect(jsonPath("$.traceId").isString())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        mvc.perform(
                        get("/api/bookings/{id}", json.readTree(result).get("bookingId").asText())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void sameRequestIdWithDifferentParametersIsConflict() throws Exception {
        var request = manual();
        service.create(alice, request);
        var changed = new CreateBooking(request.requestId(), false, 2L, null, start, end);
        mvc.perform(
                        post("/api/bookings")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(changed)))
                .andExpect(status().isConflict());
        verify(hotel, times(1)).confirm(anyLong(), any());
    }

    @Test
    void concurrentDuplicateDeliveryCreatesOnlyOneBooking() throws Exception {
        var request = manual();
        var gate = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<BookingOutcome>> futures = new ArrayList<>();
            for (int i = 0; i < 16; i++)
                futures.add(
                        pool.submit(
                                () -> {
                                    gate.await();
                                    return service.create(alice, request);
                                }));
            gate.countDown();
            Set<UUID> ids = new HashSet<>();
            for (var future : futures) ids.add(future.get(20, TimeUnit.SECONDS).booking().id());
            assertThat(ids).hasSize(1);
            assertThat(bookings.count()).isEqualTo(1);
            verify(hotel, times(1)).confirm(anyLong(), any());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void cancellationDuringConfirmationCannotConfirmCancelledBooking() throws Exception {
        var entered = new CountDownLatch(1);
        var finish = new CountDownLatch(1);
        doAnswer(
                        invocation -> {
                            entered.countDown();
                            assertThat(finish.await(10, TimeUnit.SECONDS)).isTrue();
                            return null;
                        })
                .when(hotel)
                .confirm(anyLong(), any());
        var pool = Executors.newSingleThreadExecutor();
        try {
            var result = pool.submit(() -> service.create(alice, manual()));
            assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
            UUID id = bookings.findAll().get(0).getId();
            service.cancel(alice, id);
            finish.countDown();
            assertThat(result.get(10, TimeUnit.SECONDS).booking().status())
                    .isEqualTo(BookingStatus.CANCELLED);
            assertThat(transactions.internal(id).compensationRequired()).isFalse();
        } finally {
            finish.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void adminCanCreateReadUpdateAndDeleteUser() throws Exception {
        var adminToken = JwtSupport.token(encoder, alice.toString(), "ADMIN", 3600);
        String created =
                mvc.perform(
                                post("/api/users")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                json.writeValueAsString(
                                                        new UserInput(
                                                                "managed",
                                                                "Managed123!",
                                                                Role.USER))))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long id = json.readTree(created).get("id").asLong();
        mvc.perform(get("/api/users/{id}", id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("managed"));
        mvc.perform(
                        patch("/api/users/{id}", id)
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"renamed\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(delete("/api/users/{id}", id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/users/{id}", id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void userChangesArePersistedAcrossDomainMapping() {
        userService.update(alice, new UserPatch("aliceupdated", "NewPassword123!", Role.ADMIN));
        var reloaded = userService.get(alice);
        assertThat(reloaded.username()).isEqualTo("aliceupdated");
        assertThat(reloaded.role()).isEqualTo(Role.ADMIN);
        assertThat(
                        userService
                                .login(new Credentials("aliceupdated", "NewPassword123!"))
                                .user()
                                .role())
                .isEqualTo(Role.ADMIN);
    }

    private HotelCallException remote(int status) {
        return new HotelCallException(status, "Simulated Hotel failure", null);
    }
}
