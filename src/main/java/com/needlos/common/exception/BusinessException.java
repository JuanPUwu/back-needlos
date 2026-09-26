package com.needlos.common.exception;

/** Se lanza cuando se viola una regla de negocio. Se traduce a HTTP 409. */
public class BusinessException extends RuntimeException {
    public BusinessException(String mensaje) {
        super(mensaje);
    }
}
