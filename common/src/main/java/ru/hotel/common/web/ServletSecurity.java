package ru.hotel.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.MDC;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import ru.hotel.common.ErrorDto;
import ru.hotel.common.security.JwtSupport;

import java.time.Instant;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class ServletSecurity {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper)
            throws Exception {
        var entry =
                (org.springframework.security.web.AuthenticationEntryPoint)
                        (req, res, ex) -> {
                            res.setStatus(401);
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            mapper.writeValue(
                                    res.getOutputStream(),
                                    new ErrorDto(
                                            Instant.now(),
                                            401,
                                            "Authentication required",
                                            req.getRequestURI(),
                                            MDC.get("traceId"),
                                            Map.of()));
                        };
        return http.csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                "/api/auth/**",
                                                "/user/register",
                                                "/user/auth",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/actuator/health",
                                                "/error")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(entry)
                                        .accessDeniedHandler(
                                                (req, res, ex) -> {
                                                    res.setStatus(403);
                                                    res.setContentType(
                                                            MediaType.APPLICATION_JSON_VALUE);
                                                    mapper.writeValue(
                                                            res.getOutputStream(),
                                                            new ErrorDto(
                                                                    Instant.now(),
                                                                    403,
                                                                    "Access denied",
                                                                    req.getRequestURI(),
                                                                    MDC.get("traceId"),
                                                                    Map.of()));
                                                }))
                .oauth2ResourceServer(
                        o ->
                                o.authenticationEntryPoint(entry)
                                        .jwt(
                                                j ->
                                                        j.jwtAuthenticationConverter(
                                                                JwtSupport.converter())))
                .build();
    }
}
