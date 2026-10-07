package com.needlos.security.acceso;

import com.needlos.security.rol.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Acceso: vincula una {@link com.needlos.security.cuenta.Cuenta} con una sastreria (tenant) y
 * define sus roles ahi. Una cuenta puede tener varios accesos (varias sastrerias). No usa @TenantId
 * porque se consulta durante el login, antes de que exista contexto de tenant.
 */
@Getter
@Setter
@Entity
@Table(name = "accesos")
public class Acceso {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "cuenta_id", nullable = false)
    private UUID cuentaId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn = Instant.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "acceso_roles",
            joinColumns = @JoinColumn(name = "acceso_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new HashSet<>();
}
