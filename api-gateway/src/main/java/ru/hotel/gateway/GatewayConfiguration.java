package ru.hotel.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.*;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.hotel.common.ErrorDto;
import ru.hotel.common.security.JwtSupport;

@Configuration
public class GatewayConfiguration {
    @Bean
    RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(
                        "booking-docs",
                        r ->
                                r.path("/docs/booking")
                                        .filters(f -> f.setPath("/v3/api-docs"))
                                        .uri("lb://booking-service"))
                .route(
                        "hotel-docs",
                        r ->
                                r.path("/docs/hotels")
                                        .filters(f -> f.setPath("/v3/api-docs"))
                                        .uri("lb://hotel-service"))
                .route(
                        "booking",
                        r ->
                                r.path(
                                                "/api/bookings/**",
                                                "/api/auth/**",
                                                "/api/users/**",
                                                "/booking/**",
                                                "/bookings",
                                                "/user/**")
                                        .uri("lb://booking-service"))
                .route(
                        "hotel",
                        r -> r.path("/api/hotels/**", "/api/rooms/**").uri("lb://hotel-service"))
                .build();
    }

    @Bean
    ReactiveJwtDecoder reactiveJwtDecoder(@Value("${security.jwt-secret}") String secret) {
        var decoder =
                NimbusReactiveJwtDecoder.withSecretKey(JwtSupport.key(secret))
                        .macAlgorithm(MacAlgorithm.HS256)
                        .build();
        decoder.setJwtValidator(JwtSupport.validator());
        return decoder;
    }

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ObjectMapper mapper) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(
                        a ->
                                a.pathMatchers(
                                                "/api/auth/**",
                                                "/user/register",
                                                "/user/auth",
                                                "/actuator/health",
                                                "/docs/**",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/webjars/**")
                                        .permitAll()
                                        .anyExchange()
                                        .hasAnyRole("USER", "ADMIN"))
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(
                                                (exchange, ex) ->
                                                        error(
                                                                exchange,
                                                                mapper,
                                                                401,
                                                                "Authentication required"))
                                        .accessDeniedHandler(
                                                (exchange, ex) ->
                                                        error(
                                                                exchange,
                                                                mapper,
                                                                403,
                                                                "Access denied")))
                .oauth2ResourceServer(
                        o ->
                                o.jwt(
                                                j ->
                                                        j.jwtAuthenticationConverter(
                                                                new ReactiveJwtAuthenticationConverterAdapter(
                                                                        JwtSupport.converter())))
                                        .authenticationEntryPoint(
                                                (exchange, ex) ->
                                                        error(
                                                                exchange,
                                                                mapper,
                                                                401,
                                                                "Invalid or expired token")))
                .build();
    }

    static Mono<Void> error(
            ServerWebExchange exchange, ObjectMapper mapper, int status, String message) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatusCode.valueOf(status));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            var body =
                    mapper.writeValueAsBytes(
                            new ErrorDto(
                                    Instant.now(),
                                    status,
                                    message,
                                    exchange.getRequest().getPath().value(),
                                    exchange.getAttribute("traceId"),
                                    Map.of()));
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
        } catch (java.io.IOException e) {
            return response.setComplete();
        }
    }
}
