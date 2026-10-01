package com.needlos.common.exception;

import org.springframework.http.HttpStatus;

/** La solicitud es valida pero incumple una regla de negocio. HTTP 422. */
public class ReglaNegocioException extends ApiException {

    public ReglaNegocioException(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_CONTENT;
    }
}
