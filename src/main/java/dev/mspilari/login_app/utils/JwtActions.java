package dev.mspilari.login_app.utils;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import dev.mspilari.login_app.configs.JwtConfig;

@Service
public class JwtActions {

    @Value("${jwt.expiration:300}")
    private Long jwtExpiration;

    private final JwtConfig jwtConfig;

    public JwtActions(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    public String jwtCreate(String email, String role) {
        var now = Instant.now();

        var claims = JwtClaimsSet.builder()
                .issuer("login_app")
                .subject(email)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(jwtExpiration))
                .claim("scope", role)
                .build();

        return jwtConfig.jwtEncoder().encode(JwtEncoderParameters.from(claims)).getTokenValue();

    }
}
