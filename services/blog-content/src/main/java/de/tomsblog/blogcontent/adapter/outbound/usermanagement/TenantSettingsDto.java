package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import java.util.Set;
import java.util.UUID;

/**
 * DTO representing tenant settings from the User Management Service.
 *
 * @req SWR-044
 * @req SWR-050
 * @req SWR-055
 */
public record TenantSettingsDto(
        UUID tenantId,
        String loginMode,
        boolean autoApproveOidc,
        Set<String> autoApproveEmailDomains,
        String displayName,
        String tagline,
        String impressumContent,
        String privacyPolicyContent) {}
