package com.needlos.security.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Rol global del sistema (SASTRE, SASTRE_ADMIN, SUPER_ADMIN). */
@Getter
@Setter
@Entity
@Table(name = "roles")
public class Rol {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String nombre;
}
