package com.needlos.security.sesion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Sesion de un dispositivo/navegador. Su id es el "sid" que viaja dentro del
 * access token, lo que permite revocar la sesion en el servidor (logout, cerrar
 * en otros dispositivos) de forma inmediata. El refresh token se guarda HASHEADO
 * (nunca en claro); el valor en claro solo vive en la cookie HttpOnly del cliente.
 */
@Getter
@Setter
@Entity
@Table(name = "sesiones")
public class Sesion {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "cuenta_id", nullable = false, updatable = false)
    private UUID cuentaId;

    /** Sastreria activa de la sesion; null para SUPER_ADMIN. */
    @Column(name = "tenant_id", updatable = false)
    private UUID tenantId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    /** Hash del refresh anterior a la ultima rotacion (deteccion de reuso). */
    @Column(name = "token_hash_anterior")
    private String tokenHashAnterior;

    @Column(name = "rotada_en")
    private Instant rotadaEn;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(nullable = false)
    private Instant expira;

    @Column(nullable = false)
    private boolean revocada = false;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "ultimo_uso", nullable = false)
    private Instant ultimoUso = Instant.now();

    public boolean estaActiva() {
        return !revocada && expira.isAfter(Instant.now());
    }

    public void revocar() {
        this.revocada = true;
    }
}
