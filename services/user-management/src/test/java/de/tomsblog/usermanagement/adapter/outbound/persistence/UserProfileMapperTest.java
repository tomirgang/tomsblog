package de.tomsblog.usermanagement.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.domain.model.ApprovalStatus;
import de.tomsblog.usermanagement.domain.model.AuthSource;
import de.tomsblog.usermanagement.domain.model.Role;
import de.tomsblog.usermanagement.domain.model.TenantMembership;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import de.tomsblog.usermanagement.domain.model.UserProfileId;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserProfileMapperTest {

    @Test
    @DisplayName("SWR-043: toEntity maps all OIDC fields correctly")
    void toEntityMapsOidcFieldsCorrectly() {
        TenantId tenantId = TenantId.generate();
        UserProfile profile = UserProfile.reconstitute(
                UserProfileId.generate(),
                "sub-123",
                null,
                null,
                AuthSource.OIDC,
                "user@example.com",
                "Test User",
                ApprovalStatus.APPROVED,
                Set.of(Role.READER, Role.AUTHOR),
                Set.of(new TenantMembership(tenantId, Role.ADMIN)),
                Instant.now(),
                Instant.now());

        UserProfileJpaEntity entity = UserProfileMapper.toEntity(profile);

        assertThat(entity.getId()).isEqualTo(profile.getId().value());
        assertThat(entity.getOidcSubject()).isEqualTo("sub-123");
        assertThat(entity.getUsername()).isNull();
        assertThat(entity.getPasswordHash()).isNull();
        assertThat(entity.getAuthSource()).isEqualTo("OIDC");
        assertThat(entity.getEmail()).isEqualTo("user@example.com");
        assertThat(entity.getDisplayName()).isEqualTo("Test User");
        assertThat(entity.getApprovalStatus()).isEqualTo("APPROVED");
        assertThat(entity.getGlobalRoles()).containsExactlyInAnyOrder(RoleJpa.READER, RoleJpa.AUTHOR);
        assertThat(entity.getTenantMemberships()).hasSize(1);
    }

    @Test
    @DisplayName("SWR-044: toEntity maps all internal fields correctly")
    void toEntityMapsInternalFieldsCorrectly() {
        UserProfile profile = UserProfile.reconstitute(
                UserProfileId.generate(),
                null,
                "admin",
                "hash-123",
                AuthSource.INTERNAL,
                "admin@example.com",
                "Admin",
                ApprovalStatus.PENDING,
                Set.of(Role.SUPERADMIN),
                Set.of(),
                Instant.now(),
                null);

        UserProfileJpaEntity entity = UserProfileMapper.toEntity(profile);

        assertThat(entity.getOidcSubject()).isNull();
        assertThat(entity.getUsername()).isEqualTo("admin");
        assertThat(entity.getPasswordHash()).isEqualTo("hash-123");
        assertThat(entity.getAuthSource()).isEqualTo("INTERNAL");
        assertThat(entity.getLastLoginAt()).isNull();
    }

    @Test
    @DisplayName("SWR-043: toDomain reconstitutes all fields correctly")
    void toDomainReconstitutesAllFields() {
        UUID id = UUID.randomUUID();
        UUID tenantUuid = UUID.randomUUID();
        Instant now = Instant.now();

        UserProfileJpaEntity entity = new UserProfileJpaEntity();
        entity.setId(id);
        entity.setOidcSubject("sub-456");
        entity.setUsername(null);
        entity.setPasswordHash(null);
        entity.setAuthSource("OIDC");
        entity.setEmail("user@test.com");
        entity.setDisplayName("Domain User");
        entity.setApprovalStatus("APPROVED");
        entity.setGlobalRoles(Set.of(RoleJpa.READER, RoleJpa.REVIEWER));
        entity.setTenantMemberships(Set.of(new TenantMembershipEmbeddable(tenantUuid, RoleJpa.AUTHOR)));
        entity.setCreatedAt(now);
        entity.setLastLoginAt(now);

        UserProfile profile = UserProfileMapper.toDomain(entity);

        assertThat(profile.getId().value()).isEqualTo(id);
        assertThat(profile.getOidcSubject()).isEqualTo("sub-456");
        assertThat(profile.getUsername()).isNull();
        assertThat(profile.getAuthSource()).isEqualTo(AuthSource.OIDC);
        assertThat(profile.getEmail()).isEqualTo("user@test.com");
        assertThat(profile.getDisplayName()).isEqualTo("Domain User");
        assertThat(profile.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(profile.getGlobalRoles()).containsExactlyInAnyOrder(Role.READER, Role.REVIEWER);
        assertThat(profile.getTenantMemberships()).hasSize(1);
        assertThat(profile.getCreatedAt()).isEqualTo(now);
        assertThat(profile.getLastLoginAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("SWR-043: roundtrip preserves all data")
    void roundtripPreservesAllData() {
        TenantId tenantId = TenantId.generate();
        UserProfile original = UserProfile.reconstitute(
                UserProfileId.generate(),
                "sub-rt",
                null,
                null,
                AuthSource.OIDC,
                "rt@example.com",
                "Roundtrip User",
                ApprovalStatus.APPROVED,
                Set.of(Role.READER, Role.AUTHOR),
                Set.of(new TenantMembership(tenantId, Role.ADMIN)),
                Instant.now(),
                Instant.now());

        UserProfileJpaEntity entity = UserProfileMapper.toEntity(original);
        UserProfile reconstituted = UserProfileMapper.toDomain(entity);

        assertThat(reconstituted.getId()).isEqualTo(original.getId());
        assertThat(reconstituted.getOidcSubject()).isEqualTo(original.getOidcSubject());
        assertThat(reconstituted.getAuthSource()).isEqualTo(original.getAuthSource());
        assertThat(reconstituted.getEmail()).isEqualTo(original.getEmail());
        assertThat(reconstituted.getDisplayName()).isEqualTo(original.getDisplayName());
        assertThat(reconstituted.getApprovalStatus()).isEqualTo(original.getApprovalStatus());
        assertThat(reconstituted.getGlobalRoles()).isEqualTo(original.getGlobalRoles());
        assertThat(reconstituted.getTenantMemberships()).isEqualTo(original.getTenantMemberships());
    }
}
