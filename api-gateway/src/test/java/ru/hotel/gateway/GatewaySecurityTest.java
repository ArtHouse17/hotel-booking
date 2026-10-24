package ru.hotel.gateway;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"})
@AutoConfigureWebTestClient
class GatewaySecurityTest {
    @Autowired WebTestClient client;

    @Test
    void anonymousRequestsRequireAuthenticationAndReceiveTraceId() {
        client.get()
                .uri("/api/bookings")
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectHeader()
                .exists("X-Request-Id")
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo(401);
    }

    @Test
    void internalRoutesAreHiddenEvenFromAdministrators() {
        client.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .post()
                .uri("/api/rooms/1/release")
                .exchange()
                .expectStatus()
                .isNotFound();
        client.post()
                .uri("/api/rooms/1/confirm-availability")
                .exchange()
                .expectStatus()
                .isNotFound();
    }
}
