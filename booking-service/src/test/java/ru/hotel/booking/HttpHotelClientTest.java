package ru.hotel.booking;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.client.*;
import org.springframework.web.client.*;

import ru.hotel.booking.client.*;
import ru.hotel.common.*;
import ru.hotel.common.security.JwtSupport;

import java.time.*;
import java.util.UUID;

class HttpHotelClientTest {
    private RestTemplate http;
    private MockRestServiceServer server;
    private HttpHotelClient client;
    private ReservationCommand command;

    @BeforeEach
    void setUp() {
        http = new RestTemplate();
        server = MockRestServiceServer.bindTo(http).build();
        var encoder =
                new NimbusJwtEncoder(
                        new ImmutableSecret<>(
                                JwtSupport.key("test-secret-with-at-least-thirty-two-bytes")));
        client = new HttpHotelClient(http, encoder, "http://hotel", 1);
        command =
                new ReservationCommand(
                        UUID.randomUUID(),
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusDays(2));
    }

    @Test
    void retriesServerFailureAndPreservesBookingCorrelation() {
        server.expect(
                        ExpectedCount.twice(),
                        requestTo("http://hotel/api/rooms/1/confirm-availability"))
                .andExpect(header("X-Request-Id", command.bookingId().toString()))
                .andRespond(withServerError());
        server.expect(requestTo("http://hotel/api/rooms/1/confirm-availability"))
                .andRespond(withNoContent());
        client.confirm(1L, command);
        server.verify();
    }

    @Test
    void timeoutHasThreeAttemptsAndIsReportedAsUnavailable() {
        server.expect(ExpectedCount.times(3), anything())
                .andRespond(
                        request -> {
                            throw new ResourceAccessException("Read timed out");
                        });
        assertThatThrownBy(() -> client.confirm(1L, command))
                .isInstanceOfSatisfying(
                        HotelCallException.class, e -> assertThat(e.status()).isEqualTo(503));
        server.verify();
    }

    @Test
    void conflictIsNotRetried() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> client.confirm(1L, command))
                .isInstanceOfSatisfying(
                        HotelCallException.class, e -> assertThat(e.status()).isEqualTo(409));
        server.verify();
    }

    @Test
    void missingDiscoveryInstanceIsRetriedAndReportedAsUnavailable() {
        server.expect(ExpectedCount.times(3), anything())
                .andRespond(
                        request -> {
                            throw new IllegalStateException(
                                    "No instances available for hotel-service");
                        });
        assertThatThrownBy(() -> client.confirm(1L, command))
                .isInstanceOfSatisfying(
                        HotelCallException.class, e -> assertThat(e.status()).isEqualTo(503));
        server.verify();
    }
}
