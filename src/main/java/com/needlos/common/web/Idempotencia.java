package com.needlos.common.web;

import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.SolicitudInvalidaException;
import java.util.UUID;

/**
 * Proteccion contra el doble envio (Reglas §5.8): el cliente genera un UUID por operacion y lo
 * envia en {@code Idempotency-Key}; si la misma clave llega dos veces (doble clic, reintento por
 * red), el servidor devuelve el registro ya creado en vez de crear otro.
 */
public final class Idempotencia {

    public static final String CABECERA = "Idempotency-Key";

    private Idempotencia() {}

    /** Lee la clave de la cabecera; 400 si falta o no es un UUID. */
    public static UUID clave(String cabecera) {
        if (cabecera == null || cabecera.isBlank()) {
            throw claveRequerida();
        }
        try {
            return UUID.fromString(cabecera.trim());
        } catch (IllegalArgumentException ex) {
            throw claveRequerida();
        }
    }

    private static SolicitudInvalidaException claveRequerida() {
        return new SolicitudInvalidaException(
                CodigoError.IDEMPOTENCIA_REQUERIDA,
                "Falta la clave de la operación (cabecera Idempotency-Key, un UUID).");
    }
}
