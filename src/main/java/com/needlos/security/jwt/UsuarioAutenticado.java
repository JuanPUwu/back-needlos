package com.needlos.security.jwt;

import java.util.List;
import java.util.UUID;

/**
 * Principal de Spring Security para cada peticion autenticada. Se obtiene en los controladores con
 * {@code @AuthenticationPrincipal UsuarioAutenticado}.
 *
 * @param tenantId sastreria activa; null para SUPER_ADMIN
 * @param sesionId sesion (sid) a la que pertenece el access token
 */
public record UsuarioAutenticado(UUID cuentaId, UUID tenantId, UUID sesionId, List<String> roles) {}
