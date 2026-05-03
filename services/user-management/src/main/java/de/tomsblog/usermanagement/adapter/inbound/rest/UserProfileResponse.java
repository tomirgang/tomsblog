package de.tomsblog.usermanagement.adapter.inbound.rest;

import de.tomsblog.usermanagement.domain.model.ApprovalStatus;
import de.tomsblog.usermanagement.domain.model.AuthSource;
import de.tomsblog.usermanagement.domain.model.Role;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Response representing a user profile.
 */
public record UserProfileResponse(
        String id,
        String oidcSubject,
        String username,
        AuthSource authSource,
        String email,
        String displayName,
        ApprovalStatus approvalStatus,
        Set<Role> globalRoles,
        List<TenantMembershipResponse> tenantMemberships,
        Instant createdAt,
        Instant lastLoginAt) {

    public record TenantMembershipResponse(String tenantId, Role role) {}

    public static UserProfileResponse from(UserProfile profile) {
        var memberships = profile.getTenantMemberships().stream()
                .map(m -> new TenantMembershipResponse(m.tenantId().value().toString(), m.role()))
                .toList();

        return new UserProfileResponse(
                profile.getId().asString(),
                profile.getOidcSubject(),
                profile.getUsername(),
                profile.getAuthSource(),
                profile.getEmail(),
                profile.getDisplayName(),
                profile.getApprovalStatus(),
                profile.getGlobalRoles(),
                memberships,
                profile.getCreatedAt(),
                profile.getLastLoginAt());
    }
}
