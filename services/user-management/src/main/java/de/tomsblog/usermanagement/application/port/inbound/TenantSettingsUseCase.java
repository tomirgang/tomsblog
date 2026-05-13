package de.tomsblog.usermanagement.application.port.inbound;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Inbound port for tenant settings management.
 *
 * @req SWR-044
 * @req SWR-045
 * @req SWR-050
 * @req SWR-052
 * @req SWR-053
 * @req SWR-061
 * @req SWR-069
 * @req SWR-071
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
            String tagline,
            String impressumContent,
            String privacyPolicyContent,
            String oidcIssuerUrl,
            String oidcClientId,
            String oidcClientSecret,
            String oidcButtonText,
            String defaultRole,
            String logoUrl,
            String faviconUrl,
            boolean oidcRoleMappingEnabled,
            Map<String, String> oidcRoleMappings);

    /** @req SWR-071 */
    TenantSettings updateGeneralSettings(
            TenantId tenantId,
            String displayName,
            String tagline,
            LoginMode loginMode,
            boolean autoApproveOidc,
            Set<String> autoApproveEmailDomains,
            String defaultRole,
            String logoUrl,
            String faviconUrl);

    /** @req SWR-071 */
    TenantSettings updateOidcSettings(
            TenantId tenantId,
            String oidcIssuerUrl,
            String oidcClientId,
            String oidcClientSecret,
            String oidcButtonText,
            boolean oidcRoleMappingEnabled,
            Map<String, String> oidcRoleMappings);

    /** @req SWR-071 */
    TenantSettings updateLegalSettings(TenantId tenantId, String impressumContent, String privacyPolicyContent);

    List<TenantSettings> listAllTenants();
}
