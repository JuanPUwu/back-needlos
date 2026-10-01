package com.needlos.security.sesion;

import java.time.Instant;
import java.util.UUID;

/** Una sesion abierta de la cuenta (dispositivo). "actual" marca la de esta peticion. */
public record SesionActivaResponse(
        UUID id,
        String dispositivo,
        String ip,
        Instant creadaEn,
        Instant ultimoUso,
        boolean actual
) {
}
