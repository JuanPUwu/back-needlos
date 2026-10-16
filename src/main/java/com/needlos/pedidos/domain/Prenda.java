package com.needlos.pedidos.domain;

import com.needlos.common.domain.BaseEntity;
import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.ReglaNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;

/** Linea de un pedido: una prenda (tipo, cantidad, precio) con su propio estado. */
@Getter
@Setter
@Entity
@Table(name = "prendas")
@SoftDelete(columnName = "eliminado")
public class Prenda extends BaseEntity {

    // EAGER a proposito: Hibernate no permite LAZY en un to-one cuyo destino
    // tiene @SoftDelete (Pedido). El acceso inverso es puntual (no en listados).
    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "tipo_prenda_id", nullable = false)
    private UUID tipoPrendaId;

    /** Acceso del sastre asignado; puede quedar sin asignar y cambiarse despues. */
    @Column(name = "sastre_id")
    private UUID sastreId;

    @Column(nullable = false)
    private int cantidad;

    @Column(nullable = false)
    private String descripcion;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPrenda estado = EstadoPrenda.EN_PROCESO;

    @OneToMany(mappedBy = "prenda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaCambio ASC")
    private List<EstadoPrendaHistorial> historial = new ArrayList<>();

    /** Subtotal de la linea (precio unitario x cantidad). */
    public BigDecimal subtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public boolean entregada() {
        return estado == EstadoPrenda.ENTREGADO;
    }

    /** Cambia el estado segun las transiciones permitidas y lo deja en el historial. */
    public void cambiarEstado(EstadoPrenda nuevo, UUID cuentaId) {
        if (entregada()) {
            throw new ReglaNegocioException(
                    CodigoError.PRENDA_ENTREGADA,
                    "La prenda ya fue entregada y no puede cambiar de estado.");
        }
        if (!estado.puedePasarA(nuevo)) {
            throw new ReglaNegocioException(
                    CodigoError.TRANSICION_INVALIDA,
                    nuevo == EstadoPrenda.ENTREGADO
                            ? "Solo se puede entregar una prenda finalizada."
                            : "La prenda ya está en ese estado.");
        }
        this.estado = nuevo;
        registrarEnHistorial(nuevo, cuentaId);
    }

    /** Registra un estado en el historial (estado inicial o cambio). */
    public void registrarEnHistorial(EstadoPrenda estado, UUID cuentaId) {
        EstadoPrendaHistorial h = new EstadoPrendaHistorial();
        h.setPrenda(this);
        h.setEstado(estado);
        h.setUsuarioId(cuentaId);
        historial.add(h);
    }
}
