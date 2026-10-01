package com.needlos.security.recuperacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Enlace de recuperacion de contrasena enviado por correo. Solo se guarda el
 * hash del token; el valor en claro viaja unicamente en el enlace del correo.
 * Vence a los pocos minutos y sirve una sola vez.
 */
@Getter
@Setter
@Entity
@Table(name = "recuperaciones_contrasena")
public class RecuperacionContrasena {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "cuenta_id", nullable = false, updatable = false)
    private UUID cuentaId;

    @Column(name = "token_hash", nullable = false, updatable = false)
    private String tokenHash;

    @Column(nullable = false, updatable = false)
    private Instant expira;

    @Column(name = "usado_en")
    private Instant usadoEn;

    @Column(name = "ip_address", updatable = false)
    private String ipAddress;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    public boolean vigente() {
        return usadoEn == null && expira.isAfter(Instant.now());
    }

    public void marcarUsada() {
        this.usadoEn = Instant.now();
    }
}
