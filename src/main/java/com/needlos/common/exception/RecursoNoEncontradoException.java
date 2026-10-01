package com.needlos.common.exception;

import org.springframework.http.HttpStatus;

/** El recurso no existe o pertenece a otra sastreria (no se revela cual). HTTP 404. */
public class RecursoNoEncontradoException extends ApiException {

    public RecursoNoEncontradoException(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
