package com.needlos.security.recuperacion;

/**
 * Evento: hay que enviar el correo de recuperacion. Se procesa despues del
 * commit y en segundo plano, asi la respuesta tarda lo mismo exista o no la cuenta.
 *
 * @param enlace null si la cuenta no tiene contrasena (entra con Google)
 */
public record RecuperacionSolicitada(String email, String nombre, String enlace, int minutosValidez) {

    /** Nunca exponer el enlace (lleva el token) si el evento termina en un log. */
    @Override
    public String toString() {
        return "RecuperacionSolicitada[conEnlace=" + (enlace != null) + "]";
    }
}
