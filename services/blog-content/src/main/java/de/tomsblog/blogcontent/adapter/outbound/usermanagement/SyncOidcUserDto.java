package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for syncing an OIDC user with the User Management Service.
 *
 * @req SWR-043
 */
public record SyncOidcUserDto(
        String oidcSubject, String email, String displayName, List<String> oidcGroups, UUID tenantId) {}
