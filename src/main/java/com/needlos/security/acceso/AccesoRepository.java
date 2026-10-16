package com.needlos.security.acceso;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccesoRepository extends JpaRepository<Acceso, UUID> {

    /** Accesos activos de la cuenta en sastrerias activas, con sus roles. */
    @Query(
            """
            select distinct a from Acceso a
            left join fetch a.roles
            join Tenant t on t.id = a.tenantId
            where a.cuentaId = :cuentaId and a.activo = true and t.activo = true
            """)
    List<Acceso> findVigentes(@Param("cuentaId") UUID cuentaId);

    /** Acceso activo de la cuenta a una sastreria activa, con sus roles. */
    @Query(
            """
            select a from Acceso a
            left join fetch a.roles
            join Tenant t on t.id = a.tenantId
            where a.cuentaId = :cuentaId and a.tenantId = :tenantId and a.activo = true and t.activo = true
            """)
    Optional<Acceso> findVigente(
            @Param("cuentaId") UUID cuentaId, @Param("tenantId") UUID tenantId);

    /**
     * Todos los accesos (activos o no) de una sastreria, con sus roles. Para la gestion de equipo.
     */
    @Query(
            """
            select distinct a from Acceso a
            left join fetch a.roles
            where a.tenantId = :tenantId
            """)
    List<Acceso> findDeTenant(@Param("tenantId") UUID tenantId);

    /** Un acceso de una sastreria concreta (con roles). Null si no es de esa sastreria. */
    @Query(
            """
            select a from Acceso a
            left join fetch a.roles
            where a.id = :accesoId and a.tenantId = :tenantId
            """)
    Optional<Acceso> findDeTenant(
            @Param("accesoId") UUID accesoId, @Param("tenantId") UUID tenantId);

    /** Cuenta cuantos SASTRE_ADMIN activos hay en una sastreria (para no dejarla sin admin). */
    @Query(
            """
            select count(distinct a.id) from Acceso a
            join a.roles r
            where a.tenantId = :tenantId and a.activo = true and r.nombre = 'SASTRE_ADMIN'
            """)
    long contarAdminsActivos(@Param("tenantId") UUID tenantId);

    /** Si la cuenta ya tiene un acceso (activo o no) en esa sastreria. */
    boolean existsByCuentaIdAndTenantId(UUID cuentaId, UUID tenantId);
}
