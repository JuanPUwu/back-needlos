package com.needlos.security.cuenta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CuentaRepository extends JpaRepository<Cuenta, UUID> {

    Optional<Cuenta> findByEmailIgnoreCase(String email);

    Optional<Cuenta> findByGoogleSub(String googleSub);

    boolean existsByEmailIgnoreCase(String email);
}
