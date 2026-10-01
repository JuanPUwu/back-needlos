package com.needlos.common.exception;

import org.springframework.http.HttpStatus;

/** Autenticado, pero sin permiso para la accion o la sastreria. HTTP 403. */
public class AccesoDenegadoException extends ApiException {

    public AccesoDenegadoException(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
