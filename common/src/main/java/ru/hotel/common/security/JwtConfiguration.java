package ru.hotel.common.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

@Configuration
public class JwtConfiguration {
    @Bean
    JwtDecoder jwtDecoder(@Value("${security.jwt-secret}") String secret) {
        var decoder =
                NimbusJwtDecoder.withSecretKey(JwtSupport.key(secret))
                        .macAlgorithm(MacAlgorithm.HS256)
                        .build();
        decoder.setJwtValidator(JwtSupport.validator());
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(@Value("${security.jwt-secret}") String secret) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(JwtSupport.key(secret)));
    }
}
