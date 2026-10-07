package com.needlos.tenant;

import com.needlos.tenant.domain.Tenant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    boolean existsBySlug(String slug);
}
