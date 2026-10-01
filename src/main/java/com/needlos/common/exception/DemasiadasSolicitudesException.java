package com.needlos.common.exception;

import org.springframework.http.HttpStatus;

/** Limite de intentos superado (rate limiting o bloqueo temporal). HTTP 429 con cabecera Retry-After. */
public class DemasiadasSolicitudesException extends ApiException {

    private final long reintentarEnSegundos;

    public DemasiadasSolicitudesException(long reintentarEnSegundos) {
        this(CodigoError.DEMASIADAS_SOLICITUDES,
                "Demasiados intentos. Espera un momento antes de volver a intentarlo.", reintentarEnSegundos);
    }

    public DemasiadasSolicitudesException(CodigoError codigo, String mensaje, long reintentarEnSegundos) {
        super(codigo, mensaje);
        this.reintentarEnSegundos = reintentarEnSegundos;
    }

    public long getReintentarEnSegundos() {
        return reintentarEnSegundos;
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.TOO_MANY_REQUESTS;
    }
}
