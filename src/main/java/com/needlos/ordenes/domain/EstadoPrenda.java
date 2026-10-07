package com.needlos.ordenes.domain;

/**
 * Ciclo de vida de una prenda. El orden del enum importa: se usa para derivar el estado de la orden
 * como el estado "mas inicial" entre sus prendas.
 */
public enum EstadoPrenda {
    EN_PROCESO,
    FINALIZADO,
    ENTREGADO
}
