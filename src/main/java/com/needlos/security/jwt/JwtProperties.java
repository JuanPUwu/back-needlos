package com.needlos.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Propiedades del access token JWT (prefijo needlos.jwt). */
@ConfigurationProperties(prefix = "needlos.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        String audience,
        long accessTokenMinutes
) {
}
