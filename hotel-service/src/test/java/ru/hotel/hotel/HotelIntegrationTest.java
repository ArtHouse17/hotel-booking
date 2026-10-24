package ru.hotel.hotel;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import ru.hotel.common.*;
import ru.hotel.common.security.JwtSupport;
import ru.hotel.hotel.api.HotelDtos.*;
import ru.hotel.hotel.persistence.repository.*;
import ru.hotel.hotel.service.*;

@SpringBootTest(
        properties = {
            "eureka.client.enabled=false",
            "spring.cloud.discovery.enabled=false",
            "spring.datasource.url=jdbc:h2:mem:hotel-test;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000"
        })
@AutoConfigureMockMvc
class HotelIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired InventoryService inventory;
    @Autowired ReservationService reservations;
    @Autowired ReservationJpaRepository slots;
    @Autowired RoomJpaRepository rooms;
    @Autowired HotelJpaRepository hotels;
    @Autowired JwtEncoder encoder;
    private Long roomId;
    private Long hotelId;
    private final LocalDate start = LocalDate.now().plusDays(10);
    private final LocalDate end = start.plusDays(3);

    @BeforeEach
    void setUp() {
        slots.deleteAll();
        rooms.deleteAll();
        hotels.deleteAll();
        hotelId = inventory.createHotel(new HotelInput("Test Hotel", "Moscow")).id();
        roomId = inventory.createRoom(new RoomInput(hotelId, "101", true)).id();
    }

    @Test
    void anonymousIsUnauthorizedAndUserCannotAdminister() throws Exception {
        mvc.perform(get("/api/hotels"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(
                        post("/api/hotels")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Test\",\"address\":\"City\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateUpdateAndDeleteHotel() throws Exception {
        var admin = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        mvc.perform(
                        post("/api/hotels")
                                .with(admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"New\",\"address\":\"Street\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New"));
        mvc.perform(
                        put("/api/hotels/{id}", hotelId)
                                .with(admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Updated\",\"address\":\"Street\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
        inventory.deleteRoom(roomId);
        mvc.perform(delete("/api/hotels/{id}", hotelId).with(admin))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidDatesAndInvalidPayloadReturn400() throws Exception {
        var user = jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
        mvc.perform(get("/unknown-route").with(user))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(
                        get("/api/rooms")
                                .with(user)
                                .param("startDate", end.toString())
                                .param("endDate", start.toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/hotels")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"\",\"address\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void internalEndpointRequiresServiceRole() throws Exception {
        String payload =
                "{\"bookingId\":\""
                        + UUID.randomUUID()
                        + "\",\"startDate\":\""
                        + start
                        + "\",\"endDate\":\""
                        + end
                        + "\"}";
        mvc.perform(
                        post("/api/rooms/{id}/confirm-availability", roomId)
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/rooms/{id}/confirm-availability", roomId)
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SERVICE")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isNoContent());
    }

    @Test
    void backendIndependentlyValidatesSignedAndExpiredTokens() throws Exception {
        String valid = JwtSupport.token(encoder, "1", "USER", 3600);
        mvc.perform(get("/api/hotels").header("Authorization", "Bearer " + valid))
                .andExpect(status().isOk());
        mvc.perform(
                        get("/api/hotels")
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + valid.substring(0, valid.lastIndexOf('.') + 1)
                                                + "invalid"))
                .andExpect(status().isUnauthorized());
        var expiredClaims =
                org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                        .issuer(JwtSupport.ISSUER)
                        .audience(List.of(JwtSupport.AUDIENCE))
                        .subject("1")
                        .issuedAt(Instant.now().minusSeconds(600))
                        .expiresAt(Instant.now().minusSeconds(300))
                        .claim("roles", List.of("USER"))
                        .build();
        String expired =
                encoder.encode(
                                org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                                        org.springframework.security.oauth2.jwt.JwsHeader.with(
                                                        org.springframework.security.oauth2.jose.jws
                                                                .MacAlgorithm.HS256)
                                                .build(),
                                        expiredClaims))
                        .getTokenValue();
        mvc.perform(get("/api/hotels").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void confirmationAndReleaseAreIdempotentAndLateConfirmationCannotResurrect() {
        var command = command();
        reservations.confirm(roomId, command);
        reservations.confirm(roomId, command);
        assertThat(inventory.room(roomId).timesBooked()).isEqualTo(1);
        assertThat(inventory.available(null, start, end, true)).isEmpty();

        reservations.release(roomId, command);
        reservations.release(roomId, command);
        assertThat(inventory.room(roomId).timesBooked()).isZero();
        assertThat(inventory.available(null, start, end, true)).hasSize(1);
        assertThatThrownBy(() -> reservations.confirm(roomId, command))
                .isInstanceOf(DomainConflictException.class);
    }

    @Test
    void overlappingDatesConflictButCheckoutDayCanBeReused() {
        reservations.confirm(roomId, command());
        assertThatThrownBy(
                        () ->
                                reservations.confirm(
                                        roomId,
                                        new ReservationCommand(
                                                UUID.randomUUID(),
                                                start.plusDays(1),
                                                end.plusDays(1))))
                .isInstanceOf(ApiException.class);
        reservations.confirm(
                roomId, new ReservationCommand(UUID.randomUUID(), end, end.plusDays(2)));
        assertThat(inventory.room(roomId).timesBooked()).isEqualTo(2);
    }

    @Test
    void concurrentReservationsHaveExactlyOneWinner() throws Exception {
        int requests = 24;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch gate = new CountDownLatch(1);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < requests; i++)
                results.add(
                        executor.submit(
                                () -> {
                                    gate.await();
                                    try {
                                        reservations.confirm(roomId, command());
                                        return true;
                                    } catch (ApiException e) {
                                        assertThat(e.status().value()).isEqualTo(409);
                                        return false;
                                    }
                                }));
            gate.countDown();
            int confirmed = 0;
            for (var result : results) if (result.get(30, TimeUnit.SECONDS)) confirmed++;
            assertThat(confirmed).isEqualTo(1);
            assertThat(slots.count()).isEqualTo(1);
            assertThat(inventory.room(roomId).timesBooked()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void recommendationsBalanceUsageAndExcludeMaintenance() {
        Long second = inventory.createRoom(new RoomInput(hotelId, "102", true)).id();
        inventory.createRoom(new RoomInput(hotelId, "repair", false));
        reservations.confirm(
                roomId,
                new ReservationCommand(UUID.randomUUID(), start.plusDays(30), end.plusDays(30)));
        assertThat(inventory.available(hotelId, start, end, true))
                .extracting(RoomView::id)
                .containsExactly(second, roomId);
    }

    @Test
    void roomChangesArePersistedAcrossDomainMapping() {
        inventory.updateRoom(roomId, new RoomUpdate("renovated", false));
        var reloaded = inventory.room(roomId);
        assertThat(reloaded.number()).isEqualTo("renovated");
        assertThat(reloaded.available()).isFalse();
        assertThat(inventory.available(hotelId, start, end, true)).isEmpty();
    }

    private ReservationCommand command() {
        return new ReservationCommand(UUID.randomUUID(), start, end);
    }
}
