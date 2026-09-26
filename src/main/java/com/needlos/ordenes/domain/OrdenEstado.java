package com.needlos.ordenes.domain;

/**
 * Estado de la orden (derivado, no se persiste). Una orden anulada es ANULADA;
 * en otro caso su estado es el "mas inicial" entre los estados de sus prendas
 * (si hay una EN_PROCESO y otra ENTREGADO, la orden esta EN_PROCESO).
 */
public enum OrdenEstado {
    EN_PROCESO,
    FINALIZADO,
    ENTREGADO,
    ANULADA
}
