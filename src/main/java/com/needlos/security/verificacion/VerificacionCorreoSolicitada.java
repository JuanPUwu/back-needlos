package com.needlos.security.verificacion;

/**
 * Evento: hay que enviar el codigo de verificacion por correo. Se procesa
 * despues del commit y en segundo plano (igual que la recuperacion de contrasena).
 */
public record VerificacionCorreoSolicitada(String email, String nombre, String codigo, int minutosValidez) {

    /** Nunca exponer el codigo si el evento termina en un log. */
    @Override
    public String toString() {
        return "VerificacionCorreoSolicitada[email=" + email + "]";
    }
}
