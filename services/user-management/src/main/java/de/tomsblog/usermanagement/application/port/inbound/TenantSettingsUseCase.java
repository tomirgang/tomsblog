package de.tomsblog.usermanagement.application.port.inbound;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import java.util.List;
import java.util.Set;

/**
 * Inbound port for tenant settings management.
 *
 * @req SWR-044
 * @req SWR-045
 * @req SWR-050
 * @req SWR-052
 * @req SWR-053
 */
public interface TenantSettingsUseCase {

    TenantSettings getSettings(TenantId tenantId);

    TenantSettings updateLoginMode(TenantId tenantId, LoginMode loginMode);

    TenantSettings updateAutoApproval(TenantId tenantId, boolean autoApproveOidc, Set<String> autoApproveEmailDomains);

    TenantSettings updateSettings(
            TenantId tenantId,
            LoginMode loginMode,
            boolean autoApproveOidc,
            Set<String> autoApproveEmailDomains,
            String displayName,
            String tagline);

    List<TenantSettings> listAllTenants();
}
