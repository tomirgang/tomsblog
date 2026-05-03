package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import java.util.Set;
import java.util.UUID;

/**
 * DTO representing tenant settings from the User Management Service.
 *
 * @req SWR-044
 */
public record TenantSettingsDto(
        UUID tenantId, String loginMode, boolean autoApproveOidc, Set<String> autoApproveEmailDomains) {}
