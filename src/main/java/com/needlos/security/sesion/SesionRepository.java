package com.needlos.security.sesion;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SesionRepository extends JpaRepository<Sesion, UUID> {

    /**
     * Bloquea la fila (SELECT ... FOR UPDATE): dos renovaciones simultaneas con el
     * mismo refresh se serializan y la segunda cae en la ventana de gracia.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Sesion> findByTokenHash(String tokenHash);

    Optional<Sesion> findByTokenHashAnterior(String tokenHashAnterior);

    @Query("""
            select s from Sesion s
            where s.cuentaId = :cuentaId and s.revocada = false and s.expira > :ahora
            order by s.ultimoUso desc
            """)
    List<Sesion> findActivas(@Param("cuentaId") UUID cuentaId, @Param("ahora") Instant ahora);

    @Modifying
    @Query("update Sesion s set s.revocada = true where s.cuentaId = :cuentaId and s.revocada = false")
    int revocarTodas(@Param("cuentaId") UUID cuentaId);

    @Modifying
    @Query("""
            update Sesion s set s.revocada = true
            where s.cuentaId = :cuentaId and s.id <> :excepto and s.revocada = false
            """)
    int revocarOtras(@Param("cuentaId") UUID cuentaId, @Param("excepto") UUID excepto);
}
