package com.needlos.security.filtro;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Proteccion contra fuerza bruta (prefijo needlos.rate-limit):
 *  · authPorMinuto / refreshPorMinuto: solicitudes por IP y por minuto.
 *  · intentosFallidosPorCuenta / bloqueoCuentaMinutos: bloqueo temporal del login
 *    con contrasena de un correo tras varios intentos fallidos seguidos.
 */
@ConfigurationProperties(prefix = "needlos.rate-limit")
public record RateLimitProperties(int authPorMinuto,
                                  int refreshPorMinuto,
                                  int intentosFallidosPorCuenta,
                                  int bloqueoCuentaMinutos) {
}
