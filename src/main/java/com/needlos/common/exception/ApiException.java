package com.needlos.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base de las excepciones esperadas de la aplicacion. Cada subclase fija el status HTTP; el {@link
 * CodigoError} identifica el caso concreto. El mensaje se muestra tal cual al usuario final: debe
 * ser claro y sin datos tecnicos.
 */
public abstract class ApiException extends RuntimeException {

    private final CodigoError codigo;

    protected ApiException(CodigoError codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public CodigoError getCodigo() {
        return codigo;
    }

    public abstract HttpStatus getStatus();
}
