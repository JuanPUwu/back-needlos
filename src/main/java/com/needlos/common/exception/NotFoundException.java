package com.needlos.common.exception;

/** Se lanza cuando no existe el recurso solicitado. Se traduce a HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String mensaje) {
        super(mensaje);
    }
}
