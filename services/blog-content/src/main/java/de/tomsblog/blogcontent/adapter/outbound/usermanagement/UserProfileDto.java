package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for user profile data from the User Management Service.
 *
 * @req SWR-043
 */
public record UserProfileDto(
        UUID id,
        String oidcSubject,
        String username,
        String authSource,
        String email,
        String displayName,
        String approvalStatus,
        List<String> globalRoles,
        List<TenantMembershipDto> tenantMemberships) {

    public record TenantMembershipDto(String tenantId, String role) {}
}
