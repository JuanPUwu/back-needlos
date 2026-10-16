package com.needlos.pedidos.domain;

import com.needlos.common.domain.BaseEntity;
import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.ReglaNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Pago parcial o total de un pedido. Nunca se borra: se anula (solo SASTRE_ADMIN) con motivo y
 * razon, y deja de contar para el saldo. Quien lo registro y cuando: auditoria de BaseEntity.
 */
@Getter
@Setter
@Entity
@Table(name = "abonos")
public class Abono extends BaseEntity {

    // EAGER a proposito: Hibernate no permite LAZY en un to-one cuyo destino tiene @SoftDelete.
    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoPago metodo;

    @Column(nullable = false)
    private boolean anulado = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_anulacion")
    private MotivoAnulacionAbono motivoAnulacion;

    @Column(name = "razon_anulacion")
    private String razonAnulacion;

    @Column(name = "anulado_en")
    private Instant anuladoEn;

    /** Cuenta que lo anulo. */
    @Column(name = "anulado_por")
    private UUID anuladoPor;

    @Column(name = "clave_idempotencia", nullable = false, updatable = false)
    private UUID claveIdempotencia;

    public void anular(MotivoAnulacionAbono motivo, String razon, UUID cuentaId) {
        if (anulado) {
            throw new ReglaNegocioException(
                    CodigoError.ABONO_YA_ANULADO, "El abono ya está anulado.");
        }
        this.anulado = true;
        this.motivoAnulacion = motivo;
        this.razonAnulacion = razon.trim();
        this.anuladoEn = Instant.now();
        this.anuladoPor = cuentaId;
    }
}
