package com.needlos.security.verificacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VerificacionCorreoRepository extends JpaRepository<VerificacionCorreo, UUID> {

    /** Siempre debe existir a lo mas uno (cada nuevo codigo borra el anterior). */
    Optional<VerificacionCorreo> findFirstByCuentaIdOrderByCreadoEnDesc(UUID cuentaId);

    @Modifying
    @Query("delete from VerificacionCorreo v where v.cuentaId = :cuentaId")
    int eliminarDeCuenta(@Param("cuentaId") UUID cuentaId);
}
