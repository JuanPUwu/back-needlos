package com.needlos.pedidos.domain;

import com.needlos.common.domain.BaseEntity;
import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.ReglaNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
 * Pedido de un cliente: agrupa una o mas prendas y sus abonos.
 *
 * <p>· Numero visible {@code #G-L}: G global (todas las sastrerias), L de la sastreria. · Total y
 * saldo se calculan (prendas − descuento − abonos vigentes); nunca son campos sueltos que se puedan
 * desincronizar. · Estado: derivado de las prendas ({@link #estado()}). · Anular: solo el
 * SASTRE_ADMIN, con razon, y solo si no tiene abonos vigentes (primero se registra la devolucion
 * anulando los abonos).
 */
@Getter
@Setter
@Entity
@Table(name = "pedidos")
@SoftDelete(columnName = "eliminado")
public class Pedido extends BaseEntity {

    @Column(name = "numero_global", nullable = false, updatable = false)
    private long numeroGlobal;

    /** Consecutivo de la sastreria (unico dentro del tenant, sin huecos). */
    @Column(nullable = false, updatable = false)
    private long numero;

    @Column(name = "cliente_id", nullable = false, updatable = false)
    private UUID clienteId;

    @Column(nullable = false, updatable = false)
    private LocalDate fecha;

    @Column(name = "fecha_entrega", nullable = false)
    private LocalDate fechaEntrega;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean anulado = false;

    @Column(name = "razon_anulacion")
    private String razonAnulacion;

    @Column(name = "clave_idempotencia", nullable = false, updatable = false)
    private UUID claveIdempotencia;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("creadoEn ASC")
    private List<Prenda> prendas = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    @OrderBy("creadoEn ASC")
    private List<Abono> abonos = new ArrayList<>();

    public String numeroVisible() {
        return "#" + numeroGlobal + "-" + numero;
    }

    public void agregarPrenda(Prenda prenda) {
        prenda.setPedido(this);
        prendas.add(prenda);
    }

    /** Suma de las prendas (antes de descuento). */
    public BigDecimal subtotal() {
        return prendas.stream().map(Prenda::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Total a cobrar: subtotal menos descuento (el descuento nunca supera el subtotal). */
    public BigDecimal total() {
        return subtotal().subtract(descuento).max(BigDecimal.ZERO);
    }

    /** Suma de los abonos vigentes (no anulados). */
    public BigDecimal abonado() {
        return abonos.stream()
                .filter(a -> !a.isAnulado())
                .map(Abono::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal saldo() {
        return total().subtract(abonado()).max(BigDecimal.ZERO);
    }

    /** Pagado: los abonos cubren el total (se marca solo, sin accion del usuario). */
    public boolean pagado() {
        return !anulado && saldo().signum() == 0;
    }

    /** ANULADO, o el estado mas inicial entre las prendas. */
    public EstadoPedido estado() {
        if (anulado) {
            return EstadoPedido.ANULADO;
        }
        return prendas.stream()
                .map(Prenda::getEstado)
                .min(Comparator.comparingInt(Enum::ordinal))
                .map(e -> EstadoPedido.valueOf(e.name()))
                .orElse(EstadoPedido.EN_PROCESO);
    }

    /** Activo: no anulado y con prendas sin entregar o saldo pendiente. */
    public boolean activo() {
        return !anulado && (prendas.stream().anyMatch(p -> !p.entregada()) || saldo().signum() > 0);
    }

    /** Vencido: ya paso la fecha de entrega y queda alguna prenda sin entregar. */
    public boolean vencido(LocalDate hoy) {
        return !anulado
                && fechaEntrega.isBefore(hoy)
                && prendas.stream().anyMatch(p -> !p.entregada());
    }

    /** Lanza 422 si el pedido esta anulado (no admite cambios). */
    public void verificarModificable() {
        if (anulado) {
            throw new ReglaNegocioException(
                    CodigoError.PEDIDO_ANULADO, "El pedido está anulado y no admite cambios.");
        }
    }

    /**
     * El descuento no puede superar el subtotal, y el total no puede quedar por debajo de lo
     * abonado.
     */
    public void verificarTotales() {
        if (descuento.compareTo(subtotal()) > 0) {
            throw new ReglaNegocioException(
                    CodigoError.DESCUENTO_INVALIDO,
                    "El descuento no puede ser mayor que el valor de las prendas.");
        }
        if (total().compareTo(abonado()) < 0) {
            throw new ReglaNegocioException(
                    CodigoError.TOTAL_MENOR_QUE_ABONADO,
                    "El nuevo total queda por debajo de lo ya abonado. Primero registra la"
                            + " devolución anulando abonos.");
        }
    }

    public Abono registrarAbono(BigDecimal monto, MetodoPago metodo, UUID clave) {
        verificarModificable();
        if (monto.compareTo(saldo()) > 0) {
            throw new ReglaNegocioException(
                    CodigoError.ABONO_SUPERA_SALDO,
                    "El abono no puede superar el saldo pendiente ("
                            + saldo().toPlainString()
                            + ").");
        }
        Abono abono = new Abono();
        abono.setPedido(this);
        abono.setMonto(monto);
        abono.setMetodo(metodo);
        abono.setClaveIdempotencia(clave);
        abonos.add(abono);
        return abono;
    }

    public void anular(String razon) {
        if (anulado) {
            throw new ReglaNegocioException(
                    CodigoError.PEDIDO_YA_ANULADO, "El pedido ya está anulado.");
        }
        if (abonado().signum() > 0) {
            throw new ReglaNegocioException(
                    CodigoError.PEDIDO_CON_ABONOS,
                    "El pedido tiene abonos. Antes de anularlo, registra la devolución anulando"
                            + " sus abonos.");
        }
        this.anulado = true;
        this.razonAnulacion = razon.trim();
    }
}
