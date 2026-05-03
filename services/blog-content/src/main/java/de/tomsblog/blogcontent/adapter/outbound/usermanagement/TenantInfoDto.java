package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import java.util.UUID;

/**
 * DTO representing basic tenant information.
 *
 * @req SWR-053
 */
public record TenantInfoDto(UUID tenantId, String displayName) {}
