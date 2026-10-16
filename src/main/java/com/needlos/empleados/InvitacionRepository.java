package com.needlos.empleados;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitacionRepository extends JpaRepository<Invitacion, UUID> {

    Optional<Invitacion> findByTokenHash(String tokenHash);

    Optional<Invitacion> findByIdAndTenantId(UUID id, UUID tenantId);

    /** Invitaciones pendientes (no usadas) de una sastreria, con la mas reciente primero. */
    @Query(
            """
            select i from Invitacion i
            where i.tenantId = :tenantId and i.usadoEn is null
            order by i.creadoEn desc
            """)
    List<Invitacion> findPendientes(@Param("tenantId") UUID tenantId);

    /**
     * Elimina las invitaciones pendientes de un correo en una sastreria (al reinvitar o aceptar).
     */
    @Modifying
    @Query(
            """
            delete from Invitacion i
            where i.tenantId = :tenantId and lower(i.email) = lower(:email) and i.usadoEn is null
            """)
    int eliminarPendientes(@Param("tenantId") UUID tenantId, @Param("email") String email);
}
