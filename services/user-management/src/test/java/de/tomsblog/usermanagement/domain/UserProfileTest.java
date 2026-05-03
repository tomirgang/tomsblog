package de.tomsblog.usermanagement.domain;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.domain.model.ApprovalStatus;
import de.tomsblog.usermanagement.domain.model.AuthSource;
import de.tomsblog.usermanagement.domain.model.Role;
import de.tomsblog.usermanagement.domain.model.TenantMembership;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserProfileTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("SWR-043: createFromOidc creates OIDC user with PENDING status and READER role")
        void createFromOidcSetsDefaults() {
            UserProfile profile = UserProfile.createFromOidc("sub-123", "user@example.com", "Test User");

            assertThat(profile.getId()).isNotNull();
            assertThat(profile.getOidcSubject()).isEqualTo("sub-123");
            assertThat(profile.getUsername()).isNull();
            assertThat(profile.getPasswordHash()).isNull();
            assertThat(profile.getAuthSource()).isEqualTo(AuthSource.OIDC);
            assertThat(profile.getEmail()).isEqualTo("user@example.com");
            assertThat(profile.getDisplayName()).isEqualTo("Test User");
            assertThat(profile.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
            assertThat(profile.getGlobalRoles()).containsExactly(Role.READER);
            assertThat(profile.getTenantMemberships()).isEmpty();
            assertThat(profile.getCreatedAt()).isNotNull();
            assertThat(profile.getLastLoginAt()).isNotNull();
        }

        @Test
        @DisplayName("SWR-043: createFromOidc rejects null oidcSubject")
        void createFromOidcRejectsNullSubject() {
            assertThatNullPointerException()
                    .isThrownBy(() -> UserProfile.createFromOidc(null, "user@example.com", "User"));
        }

        @Test
        @DisplayName("SWR-044: createInternal creates internal user with PENDING status and READER role")
        void createInternalSetsDefaults() {
            UserProfile profile = UserProfile.createInternal("admin", "hash123", "admin@example.com", "Admin");

            assertThat(profile.getId()).isNotNull();
            assertThat(profile.getOidcSubject()).isNull();
            assertThat(profile.getUsername()).isEqualTo("admin");
            assertThat(profile.getPasswordHash()).isEqualTo("hash123");
            assertThat(profile.getAuthSource()).isEqualTo(AuthSource.INTERNAL);
            assertThat(profile.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
            assertThat(profile.getGlobalRoles()).containsExactly(Role.READER);
        }

        @Test
        @DisplayName("SWR-044: createInternal rejects null username")
        void createInternalRejectsNullUsername() {
            assertThatNullPointerException()
                    .isThrownBy(() -> UserProfile.createInternal(null, "hash", "email", "name"));
        }

        @Test
        @DisplayName("SWR-044: createInternal rejects null passwordHash")
        void createInternalRejectsNullPasswordHash() {
            assertThatNullPointerException()
                    .isThrownBy(() -> UserProfile.createInternal("admin", null, "email", "name"));
        }
    }

    @Nested
    @DisplayName("Approval workflow")
    class ApprovalWorkflow {

        @Test
        @DisplayName("SWR-043: approve changes status to APPROVED")
        void approveChangesStatus() {
            UserProfile profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");
            assertThat(profile.isPending()).isTrue();

            profile.approve();

            assertThat(profile.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
            assertThat(profile.isApproved()).isTrue();
            assertThat(profile.isPending()).isFalse();
        }

        @Test
        @DisplayName("SWR-043: reject changes status to REJECTED")
        void rejectChangesStatus() {
            UserProfile profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");

            profile.reject();

            assertThat(profile.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
            assertThat(profile.isApproved()).isFalse();
            assertThat(profile.isPending()).isFalse();
        }
    }

    @Nested
    @DisplayName("Role management")
    class RoleManagement {

        @Test
        @DisplayName("SWR-007: assignGlobalRole adds role")
        void assignGlobalRoleAddsRole() {
            UserProfile profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");

            profile.assignGlobalRole(Role.AUTHOR);

            assertThat(profile.getGlobalRoles()).contains(Role.READER, Role.AUTHOR);
        }

        @Test
        @DisplayName("SWR-007: removeGlobalRole removes role")
        void removeGlobalRoleRemovesRole() {
            UserProfile profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");
            profile.assignGlobalRole(Role.AUTHOR);

            profile.removeGlobalRole(Role.AUTHOR);

            assertThat(profile.getGlobalRoles()).containsExactly(Role.READER);
        }

        @Test
        @DisplayName("SWR-007: assignGlobalRole rejects null")
        void assignGlobalRoleRejectsNull() {
            UserProfile profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");

            assertThatNullPointerException().isThrownBy(() -> profile.assignGlobalRole(null));
        }

        @Test
        @DisplayName("SWR-007: removeGlobalRole rejects null")
        void removeGlobalRoleRejectsNull() {
            UserProfile profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");

            assertThatNullPointerException().isThrownBy(() -> profile.removeGlobalRole(null));
        }
    }

    @Nested
    @DisplayName("Tenant memberships")
    class TenantMemberships {

        @Test
        @DisplayName("SWR-043: addTenantMembership adds membership")
        void addTenantMembership() {
            UserProfile profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");
            TenantId tenantId = TenantId.generate();

            profile.addTenantMembership(new TenantMembership(tenantId, Role.AUTHOR));

            assertThat(profile.getTenantMemberships()).hasSize(1);
            assertThat(profile.getTenantMemberships().iterator().next().tenantId())
                    .isEqualTo(tenantId);
            assertThat(profile.getTenantMemberships().iterator().next().role()).isEqualTo(Role.AUTHOR);
        }

        @Test
        @DisplayName("SWR-043: addTenantMembership replaces existing for same tenant")
        void addTenantMembershipReplacesExisting() {
            UserProfile profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");
            TenantId tenantId = TenantId.generate();

            profile.addTenantMembership(new TenantMembership(tenantId, Role.AUTHOR));
            profile.addTenantMembership(new TenantMembership(tenantId, Role.ADMIN));

            assertThat(profile.getTenantMemberships()).hasSize(1);
            assertThat(profile.getTenantMemberships().iterator().next().role()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("SWR-043: removeTenantMembership removes by tenantId")
        void removeTenantMembership() {
            UserProfile profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");
            TenantId tenantId = TenantId.generate();
            profile.addTenantMembership(new TenantMembership(tenantId, Role.AUTHOR));

            profile.removeTenantMembership(tenantId);

            assertThat(profile.getTenantMemberships()).isEmpty();
        }

        @Test
        @DisplayName("SWR-043: addTenantMembership rejects null")
        void addTenantMembershipRejectsNull() {
            UserProfile profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");

            assertThatNullPointerException().isThrownBy(() -> profile.addTenantMembership(null));
        }

        @Test
        @DisplayName("SWR-043: removeTenantMembership rejects null")
        void removeTenantMembershipRejectsNull() {
            UserProfile profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");

            assertThatNullPointerException().isThrownBy(() -> profile.removeTenantMembership(null));
        }
    }

    @Nested
    @DisplayName("Effective roles")
    class EffectiveRoles {

        @Test
        @DisplayName("SWR-007: unapproved user gets only READER")
        void unapprovedUserGetsOnlyReader() {
            UserProfile profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");
            profile.assignGlobalRole(Role.ADMIN);
            TenantId tenantId = TenantId.generate();

            Set<Role> roles = profile.getEffectiveRoles(tenantId);

            assertThat(roles).containsExactly(Role.READER);
        }

        @Test
        @DisplayName("SWR-007: approved user gets global and tenant roles")
        void approvedUserGetsGlobalAndTenantRoles() {
            UserProfile profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");
            profile.approve();
            profile.assignGlobalRole(Role.AUTHOR);
            TenantId tenantId = TenantId.generate();
            profile.addTenantMembership(new TenantMembership(tenantId, Role.ADMIN));

            Set<Role> roles = profile.getEffectiveRoles(tenantId);

            assertThat(roles).contains(Role.READER, Role.AUTHOR, Role.ADMIN);
        }

        @Test
        @DisplayName("SWR-007: approved user with no memberships gets at least READER")
        void approvedUserWithNoMembershipsGetsReader() {
            UserProfile profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");
            profile.approve();
            TenantId tenantId = TenantId.generate();

            Set<Role> roles = profile.getEffectiveRoles(tenantId);

            assertThat(roles).contains(Role.READER);
        }

        @Test
        @DisplayName("SWR-007: approved user with only global roles and no tenant membership for queried tenant")
        void approvedUserWithGlobalRolesNoTenantMembership() {
            UserProfile profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");
            profile.approve();
            profile.assignGlobalRole(Role.AUTHOR);
            TenantId otherTenantId = TenantId.generate();

            Set<Role> roles = profile.getEffectiveRoles(otherTenantId);

            assertThat(roles).contains(Role.READER, Role.AUTHOR);
        }

        @Test
        @DisplayName("SWR-007: approved user with empty global roles after removing all gets READER")
        void approvedUserWithEmptyGlobalRolesGetsReader() {
            UserProfile profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");
            profile.approve();
            profile.removeGlobalRole(Role.READER);
            TenantId tenantId = TenantId.generate();

            Set<Role> roles = profile.getEffectiveRoles(tenantId);

            assertThat(roles).containsExactly(Role.READER);
        }
    }

    @Nested
    @DisplayName("Sync operations")
    class SyncOperations {

        @Test
        @DisplayName("SWR-043: syncFromOidc updates email and displayName")
        void syncFromOidcUpdatesFields() {
            UserProfile profile = UserProfile.createFromOidc("sub-1", "old@example.com", "Old Name");

            profile.syncFromOidc("new@example.com", "New Name");

            assertThat(profile.getEmail()).isEqualTo("new@example.com");
            assertThat(profile.getDisplayName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("SWR-043: syncFromInternal updates email and displayName")
        void syncFromInternalUpdatesFields() {
            UserProfile profile = UserProfile.createInternal("admin", "hash", "old@example.com", "Old Name");

            profile.syncFromInternal("new@example.com", "New Name");

            assertThat(profile.getEmail()).isEqualTo("new@example.com");
            assertThat(profile.getDisplayName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("SWR-044: updatePasswordHash updates hash")
        void updatePasswordHashUpdatesHash() {
            UserProfile profile = UserProfile.createInternal("admin", "old-hash", "a@b.com", "Admin");

            profile.updatePasswordHash("new-hash");

            assertThat(profile.getPasswordHash()).isEqualTo("new-hash");
        }

        @Test
        @DisplayName("SWR-044: updatePasswordHash rejects null")
        void updatePasswordHashRejectsNull() {
            UserProfile profile = UserProfile.createInternal("admin", "hash", "a@b.com", "Admin");

            assertThatNullPointerException().isThrownBy(() -> profile.updatePasswordHash(null));
        }
    }
}
