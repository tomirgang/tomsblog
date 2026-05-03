package de.tomsblog.usermanagement.adapter.outbound.persistence;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.domain.model.ApprovalStatus;
import de.tomsblog.usermanagement.domain.model.AuthSource;
import de.tomsblog.usermanagement.domain.model.Role;
import de.tomsblog.usermanagement.domain.model.TenantMembership;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import de.tomsblog.usermanagement.domain.model.UserProfileId;
import java.util.stream.Collectors;

/**
 * Mapper between domain UserProfile and JPA entity.
 */
public final class UserProfileMapper {

    private UserProfileMapper() {}

    public static UserProfileJpaEntity toEntity(UserProfile profile) {
        var entity = new UserProfileJpaEntity();
        entity.setId(profile.getId().value());
        entity.setOidcSubject(profile.getOidcSubject());
        entity.setUsername(profile.getUsername());
        entity.setPasswordHash(profile.getPasswordHash());
        entity.setAuthSource(profile.getAuthSource().name());
        entity.setEmail(profile.getEmail());
        entity.setDisplayName(profile.getDisplayName());
        entity.setApprovalStatus(profile.getApprovalStatus().name());

        entity.setGlobalRoles(profile.getGlobalRoles().stream()
                .map(r -> RoleJpa.valueOf(r.name()))
                .collect(Collectors.toSet()));

        entity.setTenantMemberships(profile.getTenantMemberships().stream()
                .map(m -> new TenantMembershipEmbeddable(
                        m.tenantId().value(), RoleJpa.valueOf(m.role().name())))
                .collect(Collectors.toSet()));

        entity.setCreatedAt(profile.getCreatedAt());
        entity.setLastLoginAt(profile.getLastLoginAt());
        return entity;
    }

    public static UserProfile toDomain(UserProfileJpaEntity entity) {
        var globalRoles = entity.getGlobalRoles().stream()
                .map(r -> Role.valueOf(r.name()))
                .collect(Collectors.toSet());

        var tenantMemberships = entity.getTenantMemberships().stream()
                .map(m -> new TenantMembership(
                        TenantId.of(m.getTenantId()), Role.valueOf(m.getRole().name())))
                .collect(Collectors.toSet());

        return UserProfile.reconstitute(
                UserProfileId.of(entity.getId()),
                entity.getOidcSubject(),
                entity.getUsername(),
                entity.getPasswordHash(),
                AuthSource.valueOf(entity.getAuthSource()),
                entity.getEmail(),
                entity.getDisplayName(),
                ApprovalStatus.valueOf(entity.getApprovalStatus()),
                globalRoles,
                tenantMemberships,
                entity.getCreatedAt(),
                entity.getLastLoginAt());
    }
}
