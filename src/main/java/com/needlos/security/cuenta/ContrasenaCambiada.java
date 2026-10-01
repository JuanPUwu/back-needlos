package com.needlos.security.cuenta;

/** Evento: la contrasena de la cuenta cambio (desde "Mi cuenta" o por recuperacion). Dispara un aviso por correo. */
public record ContrasenaCambiada(String email, String nombre) {
}
