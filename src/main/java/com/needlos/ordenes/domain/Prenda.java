package com.needlos.ordenes.domain;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "prendas")
@SoftDelete(columnName = "eliminado")
public class Prenda extends BaseEntity {

    // EAGER a proposito: Hibernate no permite LAZY en un to-one cuyo destino
    // tiene @SoftDelete (Orden). El acceso inverso es puntual (no en listados).
    @ManyToOne
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    @Column(name = "tipo_prenda_id", nullable = false)
    private UUID tipoPrendaId;

    /** Sastre asignado; puede quedar sin asignar y cambiarse despues. */
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
    private List<EstadoPrendaHistorial> historial = new ArrayList<>();

    /** Subtotal de la linea (precio unitario x cantidad). */
    public BigDecimal subtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    /**
     * Cambia el estado de la prenda. Una prenda ya ENTREGADA no puede cambiar.
     * Registra el cambio en el historial.
     */
    public void cambiarEstado(EstadoPrenda nuevo, UUID usuarioId) {
        if (estado == EstadoPrenda.ENTREGADO) {
            throw new ReglaNegocioException(CodigoError.PRENDA_ENTREGADA,
                    "La prenda ya fue entregada y no puede cambiar de estado.");
        }
        this.estado = nuevo;
        registrarEnHistorial(nuevo, usuarioId);
    }

    /** Registra un estado en el historial (uso interno / estado inicial). */
    public void registrarEnHistorial(EstadoPrenda estado, UUID usuarioId) {
        EstadoPrendaHistorial h = new EstadoPrendaHistorial();
        h.setPrenda(this);
        h.setEstado(estado);
        h.setUsuarioId(usuarioId);
        historial.add(h);
    }
}
