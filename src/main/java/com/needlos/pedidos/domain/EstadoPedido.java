package com.needlos.pedidos.domain;

/**
 * Estado del pedido (derivado, no se persiste). ANULADO si se anulo; si no, el estado "mas inicial"
 * entre sus prendas (una EN_PROCESO y otra ENTREGADA → EN_PROCESO).
 */
public enum EstadoPedido {
    EN_PROCESO,
    FINALIZADO,
    ENTREGADO,
    ANULADO
}
