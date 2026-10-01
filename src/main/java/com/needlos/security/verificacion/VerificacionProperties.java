package com.needlos.security.verificacion;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Verificacion de correo al registrarse (prefijo needlos.verificacion). */
@ConfigurationProperties(prefix = "needlos.verificacion")
public record VerificacionProperties(
        int minutosValidez,
        int intentosMaximos,
        int solicitudesPorHora
) {
}
