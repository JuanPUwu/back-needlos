package com.needlos.ordenes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

/**
 * Registro inmutable de cada cambio de estado de una prenda. Da trazabilidad: quien cambio el
 * estado, cuando y a que. Alimenta informes de productividad y tiempos de entrega.
 */
@Getter
@Setter
@Entity
@Table(name = "estado_prenda_historial")
public class EstadoPrendaHistorial {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", updatable = false, nullable = false)
    private UUID tenantId;

    // EAGER a proposito: Hibernate no permite LAZY en un to-one cuyo destino
    // tiene @SoftDelete (Prenda). El acceso inverso es puntual (no en listados).
    @ManyToOne
    @JoinColumn(name = "prenda_id", nullable = false)
    private Prenda prenda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPrenda estado;

    @Column(name = "fecha_cambio", nullable = false)
    private Instant fechaCambio = Instant.now();

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @PrePersist
    void asignarId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
