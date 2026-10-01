package com.needlos.security.cuenta;

/**
 * Evento: la cuenta acaba de quedar verificada (registro con Google, o con
 * correo y contrasena tras ingresar el codigo). Dispara el correo de bienvenida.
 */
public record CuentaVerificada(String email, String nombre) {
}
