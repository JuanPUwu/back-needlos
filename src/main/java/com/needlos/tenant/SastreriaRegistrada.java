package com.needlos.tenant;

import java.util.UUID;

/**
 * Se acaba de registrar una sastreria. Se publica dentro de la misma transaccion del registro para
 * que otros modulos preparen sus datos iniciales (p. ej. el catalogo base de tipos de prenda).
 */
public record SastreriaRegistrada(UUID tenantId) {}
