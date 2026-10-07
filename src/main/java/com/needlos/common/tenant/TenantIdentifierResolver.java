package com.needlos.common.tenant;

import java.util.Map;
import java.util.UUID;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 * Le dice a Hibernate cual es el tenant "actual" en cada operacion.
 *
 * <p>Hibernate llama a {@link #resolveCurrentTenantIdentifier()} cada vez que ejecuta una consulta
 * o un insert sobre una entidad que tiene un campo anotado con {@code @TenantId}. Devolvemos el
 * tenant del request (guardado en {@link TenantContext}), de modo que:
 *
 * <p>· Los SELECT anaden automaticamente {@code WHERE tenant_id = ?}. · Los INSERT rellenan {@code
 * tenant_id} solos.
 *
 * <p>Asi es imposible "olvidar" filtrar por tenant: el aislamiento es automatico.
 */
@Component
public class TenantIdentifierResolver
        implements CurrentTenantIdentifierResolver<UUID>, HibernatePropertiesCustomizer {

    /** Clave de Hibernate para registrar el resolver (estable entre versiones). */
    private static final String TENANT_RESOLVER_KEY = "hibernate.tenant_identifier_resolver";

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return TenantContext.getTenantId();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(TENANT_RESOLVER_KEY, this);
    }
}
