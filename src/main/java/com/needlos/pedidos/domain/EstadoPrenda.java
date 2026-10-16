package com.needlos.pedidos.domain;

/**
 * Ciclo de vida de una prenda. El orden del enum importa: el estado del pedido es el "mas inicial"
 * entre sus prendas.
 *
 * <p>Transiciones: EN_PROCESO ↔ FINALIZADO (puede volver, p. ej. un ajuste) y FINALIZADO →
 * ENTREGADO. Una prenda ENTREGADA ya no cambia.
 */
public enum EstadoPrenda {
    EN_PROCESO,
    FINALIZADO,
    ENTREGADO;

    public boolean puedePasarA(EstadoPrenda nuevo) {
        return switch (this) {
            case EN_PROCESO -> nuevo == FINALIZADO;
            case FINALIZADO -> nuevo == EN_PROCESO || nuevo == ENTREGADO;
            case ENTREGADO -> false;
        };
    }
}
