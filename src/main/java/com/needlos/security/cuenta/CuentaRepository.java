package com.needlos.security.cuenta;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaRepository extends JpaRepository<Cuenta, UUID> {

    Optional<Cuenta> findByEmailIgnoreCase(String email);

    Optional<Cuenta> findByGoogleSub(String googleSub);

    boolean existsByEmailIgnoreCase(String email);
}
