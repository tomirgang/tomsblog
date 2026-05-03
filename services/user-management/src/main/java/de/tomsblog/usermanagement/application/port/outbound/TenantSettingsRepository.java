package de.tomsblog.usermanagement.application.port.outbound;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import java.util.Optional;

/**
 * Outbound port for tenant settings persistence.
 *
 * @req SWR-044
 * @req SWR-045
 */
public interface TenantSettingsRepository {

    TenantSettings save(TenantSettings tenantSettings);

    Optional<TenantSettings> findByTenantId(TenantId tenantId);
}
