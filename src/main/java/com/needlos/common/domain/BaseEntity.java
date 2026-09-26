package com.needlos.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Clase base de todas las entidades de negocio (tenant-scoped).
 *
 * Aporta, sin que cada entidad lo repita:
 *  · id UUID.
 *  · tenant_id gestionado por Hibernate (@TenantId): filtrado e insertado solo.
 *  · auditoria (quien y cuando creo/modifico) via Spring Data JPA Auditing.
 *
 * El soft-delete (columna "eliminado") se declara con @SoftDelete en cada
 * entidad concreta.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", updatable = false, nullable = false)
    private UUID tenantId;

    @CreatedDate
    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @LastModifiedDate
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    @CreatedBy
    @Column(name = "creado_por", updatable = false)
    private UUID creadoPor;

    @LastModifiedBy
    @Column(name = "actualizado_por")
    private UUID actualizadoPor;

    @PrePersist
    void asignarId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
