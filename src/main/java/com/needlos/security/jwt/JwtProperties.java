package com.needlos.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Propiedades de configuracion del JWT (prefijo needlos.jwt en application.yaml). */
@ConfigurationProperties(prefix = "needlos.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        String audience,
        long accessTokenMinutes,
        long refreshTokenDays
) {
}
