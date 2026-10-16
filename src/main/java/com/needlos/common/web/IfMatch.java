package com.needlos.common.web;

import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.ConflictoException;
import com.needlos.common.exception.SolicitudInvalidaException;

/**
 * Bloqueo optimista entre peticiones (Reglas §10): el cliente envia en la cabecera {@code If-Match}
 * la {@code version} que leyo; si el registro cambio desde entonces, la edicion se rechaza con 409
 * en vez de pisar los cambios de otra persona. Acepta {@code 3}, {@code "3"} y {@code W/"3"}.
 */
public final class IfMatch {

    public static final String CABECERA = "If-Match";

    private IfMatch() {}

    /** Lee la version de la cabecera; 400 si falta o no es un numero. */
    public static long version(String cabecera) {
        if (cabecera == null || cabecera.isBlank()) {
            throw versionRequerida();
        }
        String valor = cabecera.trim();
        if (valor.startsWith("W/")) {
            valor = valor.substring(2);
        }
        valor = valor.replace("\"", "");
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException ex) {
            throw versionRequerida();
        }
    }

    /** 409 si la version enviada no es la actual. */
    public static void verificar(long enviada, long actual) {
        if (enviada != actual) {
            throw new ConflictoException(
                    CodigoError.CONFLICTO_CONCURRENCIA,
                    "Otra persona modificó este registro mientras lo editabas. Recarga e inténtalo"
                            + " de nuevo.");
        }
    }

    private static SolicitudInvalidaException versionRequerida() {
        return new SolicitudInvalidaException(
                CodigoError.VERSION_REQUERIDA,
                "Falta la versión del registro (cabecera If-Match). Recarga e inténtalo de nuevo.");
    }
}
