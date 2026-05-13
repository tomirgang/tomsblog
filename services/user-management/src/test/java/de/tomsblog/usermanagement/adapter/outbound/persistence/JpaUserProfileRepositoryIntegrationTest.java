package de.tomsblog.usermanagement.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.domain.model.Role;
import de.tomsblog.usermanagement.domain.model.TenantMembership;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaUserProfileRepository.class)
class JpaUserProfileRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private JpaUserProfileRepository repository;

    @Test
    @DisplayName("SWR-043: Save and find OIDC user by subject")
    void saveAndFindByOidcSubject() {
        UserProfile profile = UserProfile.createFromOidc("sub-123", "user@example.com", "Test User");

        UserProfile saved = repository.save(profile);

        Optional<UserProfile> found = repository.findByOidcSubject("sub-123");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getOidcSubject()).isEqualTo("sub-123");
        assertThat(found.get().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("SWR-044: Save and find internal user by username")
    void saveAndFindByUsername() {
        UserProfile profile = UserProfile.createInternal("admin", "hash-abc", "admin@example.com", "Admin");

        UserProfile saved = repository.save(profile);

        Optional<UserProfile> found = repository.findByUsername("admin");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("SWR-043: findByOidcSubject returns empty for unknown subject")
    void findByOidcSubjectReturnsEmptyWhenNotFound() {
        Optional<UserProfile> found = repository.findByOidcSubject("unknown-subject");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("SWR-044: findByUsername returns empty for unknown username")
    void findByUsernameReturnsEmptyWhenNotFound() {
        Optional<UserProfile> found = repository.findByUsername("unknown-user");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("SWR-043: Save preserves global roles")
    void savePreservesGlobalRoles() {
        UserProfile profile = UserProfile.createFromOidc("sub-roles", "roles@example.com", "Roles User");
        profile.assignGlobalRole(Role.AUTHOR);
        profile.assignGlobalRole(Role.ADMIN);

        repository.save(profile);

        Optional<UserProfile> found = repository.findByOidcSubject("sub-roles");
        assertThat(found).isPresent();
        assertThat(found.get().getGlobalRoles()).contains(Role.READER, Role.AUTHOR, Role.ADMIN);
    }

    @Test
    @DisplayName("SWR-043: Save preserves tenant memberships")
    void savePreservesTenantMemberships() {
        UserProfile profile = UserProfile.createFromOidc("sub-tenant", "tenant@example.com", "Tenant User");
        TenantId tenantId = TenantId.generate();
        profile.addTenantMembership(new TenantMembership(tenantId, Role.AUTHOR));

        repository.save(profile);

        Optional<UserProfile> found = repository.findByOidcSubject("sub-tenant");
        assertThat(found).isPresent();
        assertThat(found.get().getTenantMemberships()).hasSize(1);
        TenantMembership membership =
                found.get().getTenantMemberships().iterator().next();
        assertThat(membership.tenantId()).isEqualTo(tenantId);
        assertThat(membership.role()).isEqualTo(Role.AUTHOR);
    }

    @Test
    @DisplayName("SWR-043: Update preserves changes")
    void updatePreservesChanges() {
        UserProfile profile = UserProfile.createFromOidc("sub-update", "old@example.com", "Old Name");
        repository.save(profile);

        Optional<UserProfile> found = repository.findByOidcSubject("sub-update");
        assertThat(found).isPresent();
        UserProfile toUpdate = found.get();
        toUpdate.syncFromOidc("new@example.com", "New Name");
        toUpdate.approve();
        repository.save(toUpdate);

        Optional<UserProfile> updated = repository.findByOidcSubject("sub-update");
        assertThat(updated).isPresent();
        assertThat(updated.get().getEmail()).isEqualTo("new@example.com");
        assertThat(updated.get().getDisplayName()).isEqualTo("New Name");
        assertThat(updated.get().isApproved()).isTrue();
    }

    @Test
    @DisplayName("SWR-051: findByTenantId returns users with membership in tenant")
    void findByTenantIdReturnsMembers() {
        TenantId tenantId = TenantId.generate();
        UserProfile member = UserProfile.createFromOidc("sub-member", "member@test.com", "Member");
        member.addTenantMembership(new TenantMembership(tenantId, Role.AUTHOR));
        repository.save(member);

        UserProfile nonMember = UserProfile.createFromOidc("sub-other", "other@test.com", "Other");
        nonMember.addTenantMembership(new TenantMembership(TenantId.generate(), Role.READER));
        repository.save(nonMember);

        List<UserProfile> found = repository.findByTenantId(tenantId);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getOidcSubject()).isEqualTo("sub-member");
    }

    @Test
    @DisplayName("SWR-051: findByTenantId returns empty list for unknown tenant")
    void findByTenantIdReturnsEmptyForUnknown() {
        List<UserProfile> found = repository.findByTenantId(TenantId.generate());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("SWR-059: existsByUsername returns true for existing user")
    void existsByUsername_returnsTrue() {
        UserProfile profile = UserProfile.createInternal("existinguser", "hash", "existing@test.com", "Existing");
        repository.save(profile);

        assertThat(repository.existsByUsername("existinguser")).isTrue();
    }

    @Test
    @DisplayName("SWR-059: existsByUsername returns false for unknown user")
    void existsByUsername_returnsFalse() {
        assertThat(repository.existsByUsername("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("SWR-059: existsByEmail returns true for existing email")
    void existsByEmail_returnsTrue() {
        UserProfile profile = UserProfile.createInternal("emailuser", "hash", "known@test.com", "Known");
        repository.save(profile);

        assertThat(repository.existsByEmail("known@test.com")).isTrue();
    }

    @Test
    @DisplayName("SWR-059: existsByEmail returns false for unknown email")
    void existsByEmail_returnsFalse() {
        assertThat(repository.existsByEmail("unknown@test.com")).isFalse();
    }

    @Test
    @DisplayName("SWR-093: delete removes OIDC user profile")
    void deleteOidcUser() {
        UserProfile profile = UserProfile.createFromOidc("sub-del-oidc", "del@example.com", "Delete Me");
        repository.save(profile);

        assertThat(repository.findByOidcSubject("sub-del-oidc")).isPresent();

        repository.delete(profile);

        assertThat(repository.findByOidcSubject("sub-del-oidc")).isEmpty();
    }

    @Test
    @DisplayName("SWR-093: delete removes internal user profile")
    void deleteInternalUser() {
        UserProfile profile = UserProfile.createInternal("del-user", "hash", "del-int@example.com", "Delete Me");
        repository.save(profile);

        assertThat(repository.findByUsername("del-user")).isPresent();

        repository.delete(profile);

        assertThat(repository.findByUsername("del-user")).isEmpty();
    }

    @Test
    @DisplayName("SWR-093: delete is no-op when user does not exist")
    void deleteNonExistentUser() {
        UserProfile profile = UserProfile.createFromOidc("non-existent-sub", "none@example.com", "Ghost");

        // Should not throw
        repository.delete(profile);
    }
}
