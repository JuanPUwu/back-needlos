package com.needlos.ordenes.domain;

import com.needlos.common.domain.BaseEntity;
import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.ReglaNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;

/**
 * Orden de trabajo de un cliente. Agrupa una o mas prendas.
 *
 * <p>· numero: consecutivo por sastreria (se muestra como #<numero>). · total: calculado (suma de
 * prendas menos descuento), nunca un campo suelto que se pueda desincronizar. · estado: derivado
 * del estado de las prendas (ver {@link #estadoActual()}). · anulacion: solo el SASTRE_ADMIN, y
 * siempre con razon.
 */
@Getter
@Setter
@Entity
@Table(name = "ordenes")
@SoftDelete(columnName = "eliminado")
public class Orden extends BaseEntity {

    /** Consecutivo visible por sastreria (unico dentro del tenant). */
    @Column(nullable = false)
    private long numero;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(nullable = false)
    private LocalDate fecha = LocalDate.now();

    @Column(name = "fecha_entrega", nullable = false)
    private LocalDate fechaEntrega;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean anulada = false;

    @Column(name = "razon_anulacion")
    private String razonAnulacion;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prenda> prendas = new ArrayList<>();

    public void agregarPrenda(Prenda prenda) {
        prenda.setOrden(this);
        prendas.add(prenda);
    }

    /** Suma de subtotales de las prendas (antes de descuento). */
    public BigDecimal subtotal() {
        return prendas.stream().map(Prenda::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Total a cobrar: subtotal menos descuento (nunca negativo). */
    public BigDecimal total() {
        BigDecimal total = subtotal().subtract(descuento == null ? BigDecimal.ZERO : descuento);
        return total.max(BigDecimal.ZERO);
    }

    /** Estado derivado: ANULADA, o el estado mas inicial entre las prendas. */
    public OrdenEstado estadoActual() {
        if (anulada) {
            return OrdenEstado.ANULADA;
        }
        return prendas.stream()
                .map(Prenda::getEstado)
                .min(Comparator.comparingInt(Enum::ordinal))
                .map(e -> OrdenEstado.valueOf(e.name()))
                .orElse(OrdenEstado.EN_PROCESO);
    }

    /**
     * Anula la orden. Exige una razon. Solo debe invocarse desde el servicio con permiso
     * SASTRE_ADMIN.
     */
    public void anular(String razon) {
        if (anulada) {
            throw new ReglaNegocioException(
                    CodigoError.PEDIDO_YA_ANULADO, "El pedido ya esta anulado.");
        }
        if (razon == null || razon.isBlank()) {
            throw new ReglaNegocioException(
                    CodigoError.VALIDACION, "Debe indicar la razon de la anulacion.");
        }
        this.anulada = true;
        this.razonAnulacion = razon.trim();
    }
}
