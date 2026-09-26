package com.needlos.security.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token de una sesion. Se guarda HASHEADO (nunca en claro): si la BD se
 * filtra, los tokens no son reutilizables. Se rota en cada refresh.
 */
@Getter
@Setter
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expira;

    @Column(nullable = false)
    private boolean revocado = false;

    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn = Instant.now();

    public boolean esValido() {
        return !revocado && expira.isAfter(Instant.now());
    }
}
