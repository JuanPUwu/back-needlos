package com.needlos.empleados;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracion de la gestion de empleados (prefijo needlos.empleados). */
@ConfigurationProperties(prefix = "needlos.empleados")
public record EmpleadoProperties(
        /* Horas que vive el enlace de invitacion para definir la contrasena. */
        int horasInvitacion) {}
