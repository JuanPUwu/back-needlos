package com.needlos.empleados;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Invitacion de un empleado a una sastreria. Mientras no se acepta, NO existe cuenta ni acceso:
 * solo esta invitacion guarda los datos y el rol para crear el acceso al aceptar. Enlace de un solo
 * uso; solo se guarda el hash del token.
 */
@Getter
@Setter
@Entity
@Table(name = "invitaciones")
public class Invitacion {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column private String telefono;

    @Column(name = "numero_documento")
    private String numeroDocumento;

    @Column(nullable = false)
    private String rol;

    @Column(name = "token_hash", nullable = false, updatable = false)
    private String tokenHash;

    @Column(nullable = false, updatable = false)
    private Instant expira;

    @Column(name = "usado_en")
    private Instant usadoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    public boolean vigente() {
        return usadoEn == null && expira.isAfter(Instant.now());
    }

    public void marcarUsada() {
        this.usadoEn = Instant.now();
    }
}
