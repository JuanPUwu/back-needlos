package com.needlos.common.exception;

import org.springframework.http.HttpStatus;

/** Credenciales o sesion invalidas. HTTP 401. */
public class NoAutenticadoException extends ApiException {

    public NoAutenticadoException(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
