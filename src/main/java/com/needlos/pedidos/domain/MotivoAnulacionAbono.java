package com.needlos.pedidos.domain;

/**
 * Por que se anula un abono. Importa para la caja (F6): una DEVOLUCION es dinero que salio; un
 * ERROR_REGISTRO es un abono que nunca entro.
 */
public enum MotivoAnulacionAbono {
    /** Se le regreso el dinero al cliente. */
    DEVOLUCION,
    /** El abono se registro por error (monto o pedido equivocado). */
    ERROR_REGISTRO
}
