package com.needlos.common.web;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Respuesta paginada estandar de la API (Reglas §5.5). Evita exponer el objeto
 * Page de Spring, cuyo formato JSON no es un contrato estable.
 */
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {

    public static <T> PaginaResponse<T> de(Page<T> page) {
        return new PaginaResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
