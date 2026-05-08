package de.tomsblog.tenantmanagement.adapter.outbound.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for tenants.
 *
 * @req SWR-072
 */
public interface SpringDataTenantRepository extends JpaRepository<TenantJpaEntity, UUID> {

    Optional<TenantJpaEntity> findByTenantId(UUID tenantId);
}
