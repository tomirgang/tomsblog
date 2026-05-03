package de.tomsblog.usermanagement.adapter.outbound.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for tenant settings.
 */
public interface SpringDataTenantSettingsRepository extends JpaRepository<TenantSettingsJpaEntity, UUID> {

    Optional<TenantSettingsJpaEntity> findByTenantId(UUID tenantId);
}
