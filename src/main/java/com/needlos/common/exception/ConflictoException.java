package com.needlos.common.exception;

import org.springframework.http.HttpStatus;

/** Conflicto con el estado actual de los datos (duplicado, edicion simultanea). HTTP 409. */
public class ConflictoException extends ApiException {

    public ConflictoException(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
