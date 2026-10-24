package ru.hotel.booking.client;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.*;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;

import ru.hotel.common.*;
import ru.hotel.common.security.JwtSupport;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

@Component
public class HttpHotelClient implements HotelPort {
    private static final Logger log = LoggerFactory.getLogger(HttpHotelClient.class);
    private final RestTemplate http;
    private final JwtEncoder encoder;
    private final String baseUrl;
    private final Retry retry;

    public HttpHotelClient(
            RestTemplate http,
            JwtEncoder encoder,
            @Value("${hotel.base-url:http://hotel-service}") String baseUrl,
            @Value("${hotel.retry-delay-ms:100}") long delay) {
        this.http = http;
        this.encoder = encoder;
        this.baseUrl = baseUrl;
        this.retry =
                Retry.of(
                        "hotel",
                        RetryConfig.custom()
                                .maxAttempts(3)
                                .intervalFunction(IntervalFunction.ofExponentialBackoff(delay, 2))
                                .retryOnException(
                                        e ->
                                                e instanceof ResourceAccessException
                                                        || e instanceof HttpServerErrorException
                                                        || e instanceof IllegalStateException)
                                .build());
        retry.getEventPublisher()
                .onRetry(
                        e ->
                                log.warn(
                                        "event=hotel_retry bookingId={} attempt={} reason={}",
                                        MDC.get("bookingId"),
                                        e.getNumberOfRetryAttempts(),
                                        e.getLastThrowable().getClass().getSimpleName()));
    }

    @Override
    public List<RoomView> recommend(Long hotelId, LocalDate start, LocalDate end) {
        var uri =
                UriComponentsBuilder.fromUriString(baseUrl + "/api/rooms/recommend")
                        .queryParam("startDate", start)
                        .queryParam("endDate", end);
        if (hotelId != null) uri.queryParam("hotelId", hotelId);
        return call(
                () ->
                        http.exchange(
                                        uri.toUriString(),
                                        HttpMethod.GET,
                                        new HttpEntity<>(headers()),
                                        new ParameterizedTypeReference<List<RoomView>>() {})
                                .getBody());
    }

    @Override
    public void confirm(Long roomId, ReservationCommand command) {
        mutate(roomId, "confirm-availability", command);
    }

    @Override
    public void release(Long roomId, ReservationCommand command) {
        mutate(roomId, "release", command);
    }

    private void mutate(Long roomId, String operation, ReservationCommand command) {
        try (var context = MDC.putCloseable("bookingId", command.bookingId().toString())) {
            call(
                    () -> {
                        var headers = headers();
                        headers.set("X-Request-Id", command.bookingId().toString());
                        http.exchange(
                                baseUrl + "/api/rooms/" + roomId + "/" + operation,
                                HttpMethod.POST,
                                new HttpEntity<>(command, headers),
                                Void.class);
                        return null;
                    });
        }
    }

    private <T> T call(Supplier<T> action) {
        try {
            return Retry.decorateSupplier(retry, action).get();
        } catch (HttpClientErrorException e) {
            throw new HotelCallException(
                    e.getStatusCode().value(), "Hotel rejected the request", e);
        } catch (RestClientException | IllegalStateException e) {
            throw new HotelCallException(503, "Hotel is temporarily unavailable", e);
        }
    }

    private HttpHeaders headers() {
        var headers = new HttpHeaders();
        headers.setBearerAuth(JwtSupport.token(encoder, "booking-service", "SERVICE", 60));
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (MDC.get("traceId") != null) headers.set("X-Request-Id", MDC.get("traceId"));
        return headers;
    }
}
