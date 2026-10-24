package ru.hotel.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@Timeout(90)
class HotelBookingE2EIT {
    private static final String PASSWORD = "E2ePassword123!";
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final String baseUrl =
            System.getProperty("e2e.base-url", "http://localhost:8080").replaceAll("/+$", "");
    private final List<String> createdBookings = new ArrayList<>();
    private final LocalDate start = LocalDate.now().plusDays(30);
    private final LocalDate end = start.plusDays(3);
    private String admin;
    private User alice;
    private User bob;
    private long hotelId;
    private long firstRoom;
    private long secondRoom;
    private long maintenanceRoom;

    @BeforeAll
    void awaitServices() throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
        int lastStatus = 0;
        while (System.nanoTime() < deadline) {
            try {
                var login =
                        api(
                                "POST",
                                "/api/auth/login",
                                null,
                                Map.of("username", "admin", "password", "Admin123!"));
                lastStatus = login.status();
                assertThat(lastStatus).as("Доступен тестовый ADMIN").isNotIn(401, 403);
                if (lastStatus == 200) {
                    admin = login.body().path("token").asText();
                    if (api("GET", "/api/hotels", admin, null).status() == 200
                            && api("GET", "/api/bookings", admin, null).status() == 200) {
                        return;
                    }
                }
            } catch (java.io.IOException e) {
                // Во время запуска Compose Gateway ещё может не принимать соединения.
            }
            Thread.sleep(1000);
        }
        throw new AssertionError(
                "Сервисы не готовы через 90 секунд: "
                        + baseUrl
                        + ", последний статус входа: "
                        + lastStatus);
    }

    @BeforeEach
    void createOwnData() throws Exception {
        createdBookings.clear();
        alice = register();
        bob = register();
        hotelId =
                expect(
                                "POST",
                                "/api/hotels",
                                admin,
                                Map.of("name", unique("E2E"), "address", "Тестовый адрес"),
                                201)
                        .body()
                        .path("id")
                        .asLong();
        firstRoom = createRoom("101", true);
        secondRoom = createRoom("102", true);
        maintenanceRoom = createRoom("ремонт", false);
    }

    @AfterEach
    void cancelOwnBookings() throws Exception {
        for (String id : createdBookings.stream().distinct().toList()) {
            var cancelled = expect("DELETE", "/api/bookings/" + id, alice.token(), null, 200);
            assertThat(cancelled.body().path("status").asText()).isEqualTo("CANCELLED");
            assertThat(cancelled.body().path("compensationRequired").asBoolean()).isFalse();
        }
    }

    @Test
    void registrationAuthenticationAndRoleRestrictions() throws Exception {
        var login =
                expect(
                        "POST",
                        "/api/auth/login",
                        null,
                        Map.of("username", alice.username(), "password", PASSWORD),
                        200);
        assertThat(login.body().path("expiresIn").asLong()).isEqualTo(3600);
        assertThat(login.body().path("user").path("role").asText()).isEqualTo("USER");
        expect("GET", "/api/hotels", login.body().path("token").asText(), null, 200);
        expect(
                "POST",
                "/api/auth/login",
                null,
                Map.of("username", alice.username(), "password", "Wrong123!"),
                401);
        expect("GET", "/api/bookings", null, null, 401);
        expect("GET", "/api/bookings", "invalid-token", null, 401);
        expect("GET", "/api/users", alice.token(), null, 403);
        expect("GET", "/api/rooms/statistics", alice.token(), null, 403);
        expect(
                "POST",
                "/api/hotels",
                alice.token(),
                Map.of("name", "Forbidden", "address", "City"),
                403);
        for (String operation : List.of("confirm-availability", "release")) {
            expect(
                    "POST",
                    "/api/rooms/" + firstRoom + "/" + operation,
                    admin,
                    Map.of(
                            "bookingId",
                            UUID.randomUUID().toString(),
                            "startDate",
                            start.toString(),
                            "endDate",
                            end.toString()),
                    404);
        }
    }

    @Test
    void administratorCanManageHotelsRoomsAndUsers() throws Exception {
        String hotelName = unique("Updated");
        expect(
                "PUT",
                "/api/hotels/" + hotelId,
                admin,
                Map.of("name", hotelName, "address", "Новый адрес"),
                200);
        assertThat(
                        expect("GET", "/api/hotels/" + hotelId, alice.token(), null, 200)
                                .body()
                                .path("name")
                                .asText())
                .isEqualTo(hotelName);
        var hotels = expect("GET", "/api/hotels?query=" + hotelName, alice.token(), null, 200);
        assertThat(ids(hotels.body().path("content"))).containsExactly(hotelId);
        expect(
                "PUT",
                "/api/rooms/" + firstRoom,
                admin,
                Map.of("number", "201", "available", false),
                200);
        assertThat(
                        expect("GET", "/api/rooms/" + firstRoom, alice.token(), null, 200)
                                .body()
                                .path("available")
                                .asBoolean())
                .isFalse();
        assertThat(
                        expect("GET", "/api/rooms/statistics?hotelId=" + hotelId, admin, null, 200)
                                .body()
                                .size())
                .isEqualTo(3);
        for (long room : List.of(firstRoom, secondRoom, maintenanceRoom)) {
            expect("DELETE", "/api/rooms/" + room, admin, null, 204);
            expect("GET", "/api/rooms/" + room, admin, null, 404);
        }
        expect("DELETE", "/api/hotels/" + hotelId, admin, null, 204);
        expect("GET", "/api/hotels/" + hotelId, admin, null, 404);

        String username = unique("managed");
        long userId =
                expect(
                                "POST",
                                "/api/users",
                                admin,
                                Map.of("username", username, "password", PASSWORD, "role", "USER"),
                                201)
                        .body()
                        .path("id")
                        .asLong();
        expect("GET", "/api/users/" + userId, admin, null, 200);
        expect(
                "PATCH",
                "/api/users/" + userId,
                admin,
                Map.of("password", "Updated123!", "role", "ADMIN"),
                200);
        var updated =
                expect(
                        "POST",
                        "/api/auth/login",
                        null,
                        Map.of("username", username, "password", "Updated123!"),
                        200);
        assertThat(updated.body().path("user").path("role").asText()).isEqualTo("ADMIN");
        expect("DELETE", "/api/users/" + userId, admin, null, 204);
        expect("GET", "/api/users/" + userId, admin, null, 404);
    }

    @Test
    void manualBookingReplayConflictAndCancellation() throws Exception {
        var payload = booking(false, firstRoom, start, end);
        var created = book(payload, 201);
        String id = created.body().path("id").asText();
        assertThat(created.body().path("status").asText()).isEqualTo("CONFIRMED");
        assertThat(created.header("X-Request-Id")).isNotBlank();
        assertThat(counter(firstRoom)).isEqualTo(1);
        assertThat(ids(available(""))).doesNotContain(firstRoom);
        assertThat(ids(available("/recommend"))).containsExactly(secondRoom);

        assertThat(book(payload, 200).body().path("id").asText()).isEqualTo(id);
        assertThat(counter(firstRoom)).isEqualTo(1);
        var changed = new HashMap<>(payload);
        changed.put("endDate", end.plusDays(1).toString());
        expect("POST", "/api/bookings", alice.token(), changed, 409);

        var conflict = book(booking(false, firstRoom, start.plusDays(1), end), 409);
        assertCancelledError(conflict, 409);
        assertThat(counter(firstRoom)).isEqualTo(1);
        for (int i = 0; i < 2; i++) {
            expect("DELETE", "/api/bookings/" + id, alice.token(), null, 200);
        }
        assertThat(counter(firstRoom)).isZero();
        assertThat(ids(available(""))).contains(firstRoom, secondRoom);
        assertThat(book(booking(false, firstRoom, start, end), 201).body().path("status").asText())
                .isEqualTo("CONFIRMED");
    }

    @Test
    void autoSelectionBalancesUsageAndIgnoresMaintenanceAndRoomId() throws Exception {
        assertThat(ids(available("/recommend"))).containsExactly(firstRoom, secondRoom);
        book(booking(false, firstRoom, start.plusDays(10), end.plusDays(10)), 201);
        assertThat(ids(available("/recommend"))).containsExactly(secondRoom, firstRoom);
        var selected = book(booking(true, -1, start, end), 201);
        assertThat(selected.body().path("roomId").asLong()).isEqualTo(secondRoom);
        assertThat(counter(firstRoom)).isEqualTo(1);
        assertThat(counter(secondRoom)).isEqualTo(1);
        assertThat(counter(maintenanceRoom)).isZero();
    }

    @Test
    void bookingsArePrivateAndInvalidDatesDoNotCreateBookings() throws Exception {
        String id = book(booking(false, firstRoom, start, end), 201).body().path("id").asText();
        expect("GET", "/api/bookings/" + id, alice.token(), null, 200);
        expect("GET", "/api/bookings/" + id, bob.token(), null, 404);
        expect("DELETE", "/api/bookings/" + id, bob.token(), null, 404);
        assertThat(
                        expect("GET", "/api/bookings", bob.token(), null, 200)
                                .body()
                                .path("totalElements")
                                .asLong())
                .isZero();
        var history = expect("GET", "/api/bookings?page=0&size=1", alice.token(), null, 200);
        assertThat(history.body().path("totalElements").asLong()).isEqualTo(1);
        assertThat(history.body().path("content").get(0).path("id").asText()).isEqualTo(id);
        expect("POST", "/api/bookings", alice.token(), booking(false, firstRoom, end, start), 400);
        expect(
                "POST",
                "/api/bookings",
                alice.token(),
                booking(false, firstRoom, start.minusDays(60), end),
                400);
        expect("POST", "/api/bookings", alice.token(), booking(false, -1, start, end), 400);
        assertThat(
                        expect("GET", "/api/bookings", alice.token(), null, 200)
                                .body()
                                .path("totalElements")
                                .asLong())
                .isEqualTo(1);
    }

    @Test
    void hotelRejectionCancelsBookingWithoutChangingCounter() throws Exception {
        var rejected = book(booking(false, maintenanceRoom, start, end), 409);
        assertCancelledError(rejected, 409);
        assertThat(counter(maintenanceRoom)).isZero();
        assertThat(ids(available(""))).containsExactlyInAnyOrder(firstRoom, secondRoom);
    }

    @Test
    void concurrentOverlappingBookingsHaveExactlyOneWinner() throws Exception {
        var requests =
                IntStream.range(0, 12)
                        .mapToObj(i -> booking(false, firstRoom, start, end))
                        .toList();
        var results = concurrently(requests);
        assertThat(results.stream().filter(r -> r.status() == 201).count()).isEqualTo(1);
        assertThat(results.stream().filter(r -> r.status() == 409).count()).isEqualTo(11);
        for (var result : results) {
            if (result.status() == 409) assertCancelledError(result, 409);
        }
        assertThat(counter(firstRoom)).isEqualTo(1);
    }

    @Test
    void concurrentDeliveryOfSameRequestCreatesOneBooking() throws Exception {
        var payload = booking(false, firstRoom, start, end);
        var results = concurrently(Collections.nCopies(12, payload));
        assertThat(results).allSatisfy(r -> assertThat(r.status()).isIn(200, 201, 202));
        assertThat(results.stream().filter(r -> r.status() == 201).count()).isEqualTo(1);
        assertThat(results.stream().map(r -> r.body().path("id").asText()).distinct().toList())
                .hasSize(1);
        String id = results.get(0).body().path("id").asText();
        assertThat(
                        expect("GET", "/api/bookings/" + id, alice.token(), null, 200)
                                .body()
                                .path("status")
                                .asText())
                .isEqualTo("CONFIRMED");
        assertThat(
                        expect("GET", "/api/bookings", alice.token(), null, 200)
                                .body()
                                .path("totalElements")
                                .asLong())
                .isEqualTo(1);
        assertThat(counter(firstRoom)).isEqualTo(1);
    }

    private User register() throws Exception {
        String name = unique("e2e");
        var registered =
                expect(
                                "POST",
                                "/api/auth/register",
                                null,
                                Map.of("username", name, "password", PASSWORD, "role", "ADMIN"),
                                201)
                        .body();
        assertThat(registered.path("user").path("role").asText()).isEqualTo("USER");
        return new User(name, registered.path("token").asText());
    }

    private long createRoom(String number, boolean available) throws Exception {
        return expect(
                        "POST",
                        "/api/rooms",
                        admin,
                        Map.of("hotelId", hotelId, "number", number, "available", available),
                        201)
                .body()
                .path("id")
                .asLong();
    }

    private Map<String, Object> booking(boolean auto, long room, LocalDate from, LocalDate to) {
        return Map.of(
                "requestId",
                UUID.randomUUID().toString(),
                "autoSelect",
                auto,
                "roomId",
                room,
                "hotelId",
                hotelId,
                "startDate",
                from.toString(),
                "endDate",
                to.toString());
    }

    private Response book(Map<String, Object> payload, int status) throws Exception {
        var response = expect("POST", "/api/bookings", alice.token(), payload, status);
        remember(response);
        return response;
    }

    private List<Response> concurrently(List<Map<String, Object>> payloads) throws Exception {
        List<CompletableFuture<HttpResponse<String>>> pending = new ArrayList<>();
        for (var payload : payloads) {
            pending.add(
                    http.sendAsync(
                            request("POST", "/api/bookings", alice.token(), payload),
                            HttpResponse.BodyHandlers.ofString()));
        }
        CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new))
                .get(40, TimeUnit.SECONDS);
        List<Response> results = new ArrayList<>();
        for (var future : pending) {
            var response = response(future.join());
            remember(response);
            results.add(response);
        }
        return results;
    }

    private void remember(Response response) {
        if (response.status() >= 200
                && response.status() < 300
                && response.body().hasNonNull("id")) {
            createdBookings.add(response.body().path("id").asText());
        }
    }

    private void assertCancelledError(Response error, int status) throws Exception {
        assertThat(error.body().path("status").asInt()).isEqualTo(status);
        assertThat(error.body().path("traceId").asText()).isEqualTo(error.header("X-Request-Id"));
        String id = error.body().path("bookingId").asText();
        assertThat(id).isNotBlank();
        var cancelled = expect("GET", "/api/bookings/" + id, alice.token(), null, 200).body();
        assertThat(cancelled.path("status").asText()).isEqualTo("CANCELLED");
        assertThat(cancelled.path("compensationRequired").asBoolean()).isFalse();
    }

    private JsonNode available(String suffix) throws Exception {
        return expect(
                        "GET",
                        "/api/rooms"
                                + suffix
                                + "?hotelId="
                                + hotelId
                                + "&startDate="
                                + start
                                + "&endDate="
                                + end,
                        alice.token(),
                        null,
                        200)
                .body();
    }

    private List<Long> ids(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false)
                .map(room -> room.path("id").asLong())
                .toList();
    }

    private long counter(long room) throws Exception {
        return expect("GET", "/api/rooms/" + room, alice.token(), null, 200)
                .body()
                .path("timesBooked")
                .asLong();
    }

    private Response expect(String method, String path, String token, Object body, int status)
            throws Exception {
        var response = api(method, path, token, body);
        assertThat(response.status()).as("%s %s", method, path).isEqualTo(status);
        return response;
    }

    private Response api(String method, String path, String token, Object body) throws Exception {
        return response(
                http.send(
                        request(method, path, token, body), HttpResponse.BodyHandlers.ofString()));
    }

    private HttpRequest request(String method, String path, String token, Object body)
            throws Exception {
        var request =
                HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(25));
        if (token != null) request.header("Authorization", "Bearer " + token);
        var publisher = HttpRequest.BodyPublishers.noBody();
        if (body != null) {
            request.header("Content-Type", "application/json");
            publisher = HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body));
        }
        return request.method(method, publisher).build();
    }

    private Response response(HttpResponse<String> response) throws Exception {
        var body = response.body().isBlank() ? json.nullNode() : json.readTree(response.body());
        return new Response(response.statusCode(), body, response.headers());
    }

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private record User(String username, String token) {}

    private record Response(int status, JsonNode body, HttpHeaders headers) {
        String header(String name) {
            return headers.firstValue(name).orElse("");
        }
    }
}
