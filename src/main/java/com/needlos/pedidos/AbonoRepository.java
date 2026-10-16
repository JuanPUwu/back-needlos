package com.needlos.pedidos;

import com.needlos.pedidos.domain.Abono;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbonoRepository extends JpaRepository<Abono, UUID> {

    Optional<Abono> findByClaveIdempotencia(UUID clave);
}
