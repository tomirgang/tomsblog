package de.tomsblog.tenantmanagement.application.port.outbound;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.tenantmanagement.domain.model.Tenant;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for tenant persistence (ADR-0031).
 *
 * @req SWR-072
 */
public interface TenantRepository {

    Optional<Tenant> findByTenantId(TenantId tenantId);

    Tenant save(Tenant tenant);

    List<Tenant> findAll();
}
