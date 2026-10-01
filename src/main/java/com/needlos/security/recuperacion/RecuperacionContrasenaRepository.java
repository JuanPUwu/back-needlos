package com.needlos.security.recuperacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RecuperacionContrasenaRepository extends JpaRepository<RecuperacionContrasena, UUID> {

    Optional<RecuperacionContrasena> findByTokenHash(String tokenHash);

    /** Invalida los enlaces aun no usados de la cuenta (solo el ultimo enlace sirve). */
    @Modifying
    @Query("delete from RecuperacionContrasena r where r.cuentaId = :cuentaId and r.usadoEn is null")
    int eliminarPendientes(@Param("cuentaId") UUID cuentaId);
}
