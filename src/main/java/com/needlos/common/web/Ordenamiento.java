package com.needlos.common.web;

import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.SolicitudInvalidaException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Lista blanca de campos por los que se permite ordenar un listado (Reglas §5.6).
 * Un campo no permitido responde 400 en lugar de llegar a la consulta.
 */
public final class Ordenamiento {

    private Ordenamiento() {
    }

    public static Pageable validar(Pageable pageable, Set<String> camposPermitidos) {
        for (Sort.Order orden : pageable.getSort()) {
            if (!camposPermitidos.contains(orden.getProperty())) {
                throw new SolicitudInvalidaException(CodigoError.ORDENAMIENTO_NO_PERMITIDO,
                        "No se puede ordenar por '" + orden.getProperty() + "'. Campos permitidos: "
                                + String.join(", ", camposPermitidos.stream().sorted().toList()) + ".");
            }
        }
        return pageable;
    }
}
