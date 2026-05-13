package de.tomsblog.tenantmanagement.application.port.inbound;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.tenantmanagement.domain.model.Tenant;
import java.util.List;
import java.util.Map;

/**
 * Inbound port for tenant management operations (ADR-0031).
 *
 * @req SWR-072
 * @req SWR-073
 * @req SWR-074
 */
public interface TenantManagementUseCase {

    Tenant createTenant(String slug, String displayName);

    Tenant getTenant(TenantId tenantId);

    Tenant updateGeneralSettings(
            TenantId tenantId,
            String displayName,
            String tagline,
            String loginMode,
            boolean autoApproveOidc,
            java.util.Set<String> autoApproveEmailDomains,
            String defaultRole,
            String logoUrl,
            String faviconUrl);

    Tenant updateOidcSettings(
            TenantId tenantId,
            String oidcIssuerUrl,
            String oidcClientId,
            String oidcClientSecret,
            String oidcButtonText,
            boolean oidcRoleMappingEnabled,
            Map<String, String> oidcRoleMappings);

    Tenant updateLegalSettings(TenantId tenantId, String impressumContent, String privacyPolicyContent);

    List<Tenant> listAllTenants();
}
