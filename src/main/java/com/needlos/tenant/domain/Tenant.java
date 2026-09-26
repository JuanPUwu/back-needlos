package com.needlos.tenant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Una sastreria que usa el sistema. Los tenants no se filtran por tenant.
 * El plan arranca en DEMO (auto-registro) y el SUPER_ADMIN puede otorgar una
 * licencia que lo pasa a un plan completo por un periodo.
 */
@Getter
@Setter
@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanTenant plan = PlanTenant.DEMO;

    /** Fin de la licencia otorgada por el SUPER_ADMIN (null si sigue en DEMO). */
    @Column(name = "licencia_hasta")
    private Instant licenciaHasta;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn = Instant.now();
}
