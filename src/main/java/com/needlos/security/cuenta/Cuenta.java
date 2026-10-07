package com.needlos.security.cuenta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Cuenta: identidad global de una persona (unica por correo). Guarda las credenciales (contrasena
 * y/o vinculo con Google) y sus datos personales. NO esta atada a una sastreria: el vinculo con
 * cada sastreria es un {@link com.needlos.security.acceso.Acceso}. Una cuenta puede acceder a
 * varias.
 */
@Getter
@Setter
@Entity
@Table(name = "cuentas")
public class Cuenta {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    /** Null si la cuenta es solo-Google (sin contrasena). */
    @Column(name = "password_hash")
    private String passwordHash;

    /** Identificador de Google (sub) si la cuenta esta vinculada; null si no. */
    @Column(name = "google_sub", unique = true)
    private String googleSub;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(name = "numero_documento")
    private String numeroDocumento;

    @Column private String telefono;

    @Column(nullable = false)
    private boolean activo = true;

    /**
     * SUPER_ADMIN del sistema (el dueno de Needlos). Es global: NO pertenece a ninguna sastreria,
     * ve la info de todas. Se representa aqui (en la cuenta) y no como un rol de acceso.
     */
    @Column(name = "super_admin", nullable = false)
    private boolean superAdmin = false;

    /**
     * Correo verificado. TRUE por defecto: solo el auto-registro con correo y contrasena lo pone en
     * FALSE (exige el codigo de 6 digitos); Google ya verifica el correo, y las demas cuentas
     * (empleados, semillas) no lo necesitan.
     */
    @Column(nullable = false)
    private boolean verificada = true;

    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn = Instant.now();
}
