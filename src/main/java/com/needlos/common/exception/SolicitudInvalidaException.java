package com.needlos.common.exception;

import org.springframework.http.HttpStatus;

/** Parametros de la solicitud no permitidos (p. ej. ordenamiento por un campo no autorizado). HTTP 400. */
public class SolicitudInvalidaException extends ApiException {

    public SolicitudInvalidaException(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
