package com.needlos.common.config;

import com.needlos.common.tenant.TenantContext;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Habilita la auditoria automatica de JPA. El "auditor" (quien realiza la accion) es el usuario del
 * request actual, que leemos de {@link TenantContext}.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaConfig {

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return () -> Optional.ofNullable(TenantContext.getUsuarioId());
    }
}
