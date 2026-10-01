package com.needlos.security.verificacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Codigo de 6 digitos para verificar el correo de una cuenta registrada con
 * contrasena (Google ya verifica el correo, no pasa por aqui). Solo se guarda
 * el hash; a diferencia de un token largo, un codigo de 6 digitos SI se puede
 * adivinar por fuerza bruta, por eso lleva su propio contador de intentos
 * fallidos (ver {@link VerificacionCorreoService}).
 */
@Getter
@Setter
@Entity
@Table(name = "verificaciones_correo")
public class VerificacionCorreo {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "cuenta_id", nullable = false, updatable = false)
    private UUID cuentaId;

    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;

    @Column(nullable = false)
    private Instant expira;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    public boolean vigente() {
        return expira.isAfter(Instant.now());
    }
}
