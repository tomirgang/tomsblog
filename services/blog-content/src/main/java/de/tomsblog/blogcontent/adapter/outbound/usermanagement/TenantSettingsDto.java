package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * DTO representing tenant settings from the User Management Service.
 *
 * @req SWR-044
 * @req SWR-050
 * @req SWR-055
 * @req SWR-061
 * @req SWR-091
 * @req SWR-092
 * @req SWR-094
 * @req SWR-095
 */
public record TenantSettingsDto(
        UUID tenantId,
        String loginMode,
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
        Map<String, String> oidcRoleMappings) {

    /**
     * Returns whether an OIDC client secret is configured (for UI display).
     */
    public boolean hasOidcClientSecret() {
        return oidcClientSecret != null && !oidcClientSecret.isEmpty();
    }
}
