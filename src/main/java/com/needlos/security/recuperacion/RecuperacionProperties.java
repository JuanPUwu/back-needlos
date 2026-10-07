package com.needlos.security.recuperacion;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Recuperacion de contrasena (prefijo needlos.recuperacion). */
@ConfigurationProperties(prefix = "needlos.recuperacion")
public record RecuperacionProperties(
        /* Base publica del frontend para armar el enlace, p. ej. https://needlos.com */
        String urlFrontend, int minutosValidez, int solicitudesPorHora) {}
