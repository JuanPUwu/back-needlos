package com.needlos.security.cuenta;

/**
 * Evento: el login con contrasena de la cuenta se bloqueo por intentos fallidos. Dispara un aviso
 * por correo.
 */
public record CuentaBloqueada(String email, String nombre, int minutos) {}
