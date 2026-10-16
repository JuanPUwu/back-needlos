package com.needlos.common.consecutivo;

/**
 * Series numeradas. Cada una tiene un contador por sastreria (sin huecos) y, si aplica, una
 * secuencia global de PostgreSQL compartida por todas las sastrerias (puede tener huecos).
 */
public enum TipoConsecutivo {
    PEDIDO("pedidos_numero_global_seq");

    private final String secuenciaGlobal;

    TipoConsecutivo(String secuenciaGlobal) {
        this.secuenciaGlobal = secuenciaGlobal;
    }

    String secuenciaGlobal() {
        return secuenciaGlobal;
    }
}
