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
}
