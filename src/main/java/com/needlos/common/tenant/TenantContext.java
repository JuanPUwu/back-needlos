package com.needlos.common.tenant;

import java.util.UUID;

/**
 * Guarda el tenant y el usuario del request actual en un {@link ThreadLocal}.
 *
 * El filtro de autenticacion ({@code JwtAuthenticationFilter}) lo rellena al
 * validar el token, y se limpia al terminar cada request. A partir de aqui:
 *
 *  · Hibernate lee {@link #getTenantId()} (via TenantIdentifierResolver) para
 *    filtrar automaticamente TODAS las consultas por tenant.
 *  · La auditoria JPA lee {@link #getUsuarioId()} para rellenar creado_por /
 *    actualizado_por.
 */
public final class TenantContext {

    /** UUID "cero": se usa cuando aun no hay tenant resuelto (endpoints publicos). */
    public static final UUID SIN_TENANT = new UUID(0L, 0L);

    private static final ThreadLocal<UUID> TENANT  = new ThreadLocal<>();
    private static final ThreadLocal<UUID> USUARIO = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId, UUID usuarioId) {
        TENANT.set(tenantId);
        USUARIO.set(usuarioId);
    }

    public static UUID getTenantId() {
        UUID t = TENANT.get();
        return t != null ? t : SIN_TENANT;
    }

    public static UUID getUsuarioId() {
        return USUARIO.get();
    }

    public static void clear() {
        TENANT.remove();
        USUARIO.remove();
    }
}
