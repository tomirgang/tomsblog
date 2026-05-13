package de.tomsblog.usermanagement.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.tomsblog.shared.audit.AuditLogger;
import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.RegisterUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.SyncInternalUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.SyncOidcUserCommand;
import de.tomsblog.usermanagement.application.port.outbound.TenantSettingsRepository;
import de.tomsblog.usermanagement.application.port.outbound.UserProfileRepository;
import de.tomsblog.usermanagement.domain.model.ApprovalStatus;
import de.tomsblog.usermanagement.domain.model.Role;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository repository;

    @Mock
    private TenantSettingsRepository tenantSettingsRepository;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserProfileService service;

    private static final TenantId TENANT_ID = TenantId.generate();

    @BeforeEach
    void setUp() {
        service = new UserProfileService(repository, tenantSettingsRepository, auditLogger, passwordEncoder);
    }

    @Nested
    @DisplayName("syncFromOidc")
    class SyncFromOidc {

        @Test
        @DisplayName("SWR-043: creates new profile for unknown OIDC subject")
        void createsNewProfileForUnknownSubject() {
            when(repository.findByOidcSubject("sub-1")).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(tenantSettingsRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());

            var command = new SyncOidcUserCommand("sub-1", "user@example.com", "User", List.of(), TENANT_ID);
            var result = service.syncFromOidc(command);

            assertThat(result.getOidcSubject()).isEqualTo("sub-1");
            assertThat(result.getEmail()).isEqualTo("user@example.com");
            verify(repository).save(any(UserProfile.class));
        }

        @Test
        @DisplayName("SWR-043: updates existing profile for known OIDC subject")
        void updatesExistingProfileForKnownSubject() {
            var existing = UserProfile.createFromOidc("sub-1", "old@example.com", "Old Name");
            when(repository.findByOidcSubject("sub-1")).thenReturn(Optional.of(existing));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new SyncOidcUserCommand("sub-1", "new@example.com", "New Name", List.of(), TENANT_ID);
            var result = service.syncFromOidc(command);

            assertThat(result.getEmail()).isEqualTo("new@example.com");
            assertThat(result.getDisplayName()).isEqualTo("New Name");
            verify(repository).save(existing);
        }

        @Test
        @DisplayName("SWR-045: auto-approves new OIDC profile when autoApproveOidc enabled")
        void autoApprovesOidcProfile() {
            when(repository.findByOidcSubject("sub-2")).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            var settings = de.tomsblog.usermanagement.domain.model.TenantSettings.reconstitute(
                    TENANT_ID,
                    de.tomsblog.usermanagement.domain.model.LoginMode.BOTH,
                    true,
                    java.util.Set.of(),
                    "Toms Blog",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    Map.of());
            when(tenantSettingsRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));

            var command = new SyncOidcUserCommand("sub-2", "user@any.com", "User", List.of(), TENANT_ID);
            var result = service.syncFromOidc(command);

            assertThat(result.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        }

        @Test
        @DisplayName("SWR-045: auto-approves new OIDC profile when email domain matches")
        void autoApprovesEmailDomainMatch() {
            when(repository.findByOidcSubject("sub-3")).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            var settings = de.tomsblog.usermanagement.domain.model.TenantSettings.reconstitute(
                    TENANT_ID,
                    de.tomsblog.usermanagement.domain.model.LoginMode.BOTH,
                    false,
                    java.util.Set.of("company.com"),
                    "Toms Blog",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    Map.of());
            when(tenantSettingsRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));

            var command = new SyncOidcUserCommand("sub-3", "user@company.com", "User", List.of(), TENANT_ID);
            var result = service.syncFromOidc(command);

            assertThat(result.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        }
    }

    @Nested
    @DisplayName("syncFromInternal")
    class SyncFromInternal {

        @Test
        @DisplayName("SWR-044: creates new profile for unknown username")
        void createsNewProfileForUnknownUsername() {
            when(repository.findByUsername("admin")).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(tenantSettingsRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());

            var command = new SyncInternalUserCommand("admin", "hash", "admin@example.com", "Admin", TENANT_ID);
            var result = service.syncFromInternal(command);

            assertThat(result.getUsername()).isEqualTo("admin");
            verify(repository).save(any(UserProfile.class));
        }

        @Test
        @DisplayName("SWR-044: updates existing profile for known username")
        void updatesExistingProfileForKnownUsername() {
            var existing = UserProfile.createInternal("admin", "old-hash", "old@example.com", "Old");
            when(repository.findByUsername("admin")).thenReturn(Optional.of(existing));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new SyncInternalUserCommand("admin", "new-hash", "new@example.com", "New", TENANT_ID);
            var result = service.syncFromInternal(command);

            assertThat(result.getEmail()).isEqualTo("new@example.com");
            assertThat(result.getPasswordHash()).isEqualTo("new-hash");
            verify(repository).save(existing);
        }
    }

    @Nested
    @DisplayName("findByOidcSubject")
    class FindByOidcSubject {

        @Test
        @DisplayName("SWR-043: returns profile when found")
        void returnsProfileWhenFound() {
            var profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");
            when(repository.findByOidcSubject("sub-1")).thenReturn(Optional.of(profile));

            var result = service.findByOidcSubject("sub-1");

            assertThat(result).isEqualTo(profile);
        }

        @Test
        @DisplayName("SWR-043: throws when not found")
        void throwsWhenNotFound() {
            when(repository.findByOidcSubject("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByOidcSubject("unknown"))
                    .isInstanceOf(UserProfileNotFoundException.class)
                    .hasMessageContaining("unknown");
        }
    }

    @Nested
    @DisplayName("findByUsername")
    class FindByUsername {

        @Test
        @DisplayName("SWR-043: returns profile when found")
        void returnsProfileWhenFound() {
            var profile = UserProfile.createInternal("admin", "hash", "a@b.com", "Admin");
            when(repository.findByUsername("admin")).thenReturn(Optional.of(profile));

            var result = service.findByUsername("admin");

            assertThat(result).isEqualTo(profile);
        }

        @Test
        @DisplayName("SWR-043: throws when not found")
        void throwsWhenNotFound() {
            when(repository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByUsername("unknown"))
                    .isInstanceOf(UserProfileNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Approval operations")
    class ApprovalOperations {

        @Test
        @DisplayName("SWR-043: approveUser approves via OIDC subject")
        void approveUserByOidcSubject() {
            var profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");
            when(repository.findByOidcSubject("sub-1")).thenReturn(Optional.of(profile));

            service.approveUser("sub-1");

            assertThat(profile.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
            verify(repository).save(profile);
        }

        @Test
        @DisplayName("SWR-043: approveUser finds by username if OIDC not found")
        void approveUserByUsername() {
            var profile = UserProfile.createInternal("admin", "hash", "a@b.com", "Admin");
            when(repository.findByOidcSubject("admin")).thenReturn(Optional.empty());
            when(repository.findByUsername("admin")).thenReturn(Optional.of(profile));

            service.approveUser("admin");

            assertThat(profile.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
            verify(repository).save(profile);
        }

        @Test
        @DisplayName("SWR-043: rejectUser rejects via identifier")
        void rejectUser() {
            var profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");
            when(repository.findByOidcSubject("sub-1")).thenReturn(Optional.of(profile));

            service.rejectUser("sub-1");

            assertThat(profile.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
            verify(repository).save(profile);
        }

        @Test
        @DisplayName("SWR-043: approveUser throws when user not found by either identifier")
        void approveUserThrowsWhenNotFound() {
            when(repository.findByOidcSubject("unknown")).thenReturn(Optional.empty());
            when(repository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.approveUser("unknown")).isInstanceOf(UserProfileNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Role assignment")
    class RoleAssignment {

        @Test
        @DisplayName("SWR-007: assignGlobalRole assigns role")
        void assignGlobalRole() {
            var profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");
            when(repository.findByOidcSubject("sub-1")).thenReturn(Optional.of(profile));

            service.assignGlobalRole("sub-1", Role.ADMIN);

            assertThat(profile.getGlobalRoles()).contains(Role.ADMIN);
            verify(repository).save(profile);
        }

        @Test
        @DisplayName("SWR-007: removeGlobalRole removes role")
        void removeGlobalRole() {
            var profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");
            profile.assignGlobalRole(Role.ADMIN);
            when(repository.findByOidcSubject("sub-1")).thenReturn(Optional.of(profile));

            service.removeGlobalRole("sub-1", Role.ADMIN);

            assertThat(profile.getGlobalRoles()).doesNotContain(Role.ADMIN);
            verify(repository).save(profile);
        }
    }

    @Nested
    @DisplayName("Tenant membership management")
    class TenantMembershipManagement {

        @Test
        @DisplayName("SWR-043: addTenantMembership adds membership")
        void addTenantMembership() {
            var profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");
            var tenantId = TenantId.generate();
            when(repository.findByOidcSubject("sub-1")).thenReturn(Optional.of(profile));

            service.addTenantMembership("sub-1", tenantId, Role.AUTHOR);

            assertThat(profile.getTenantMemberships()).hasSize(1);
            verify(repository).save(profile);
        }

        @Test
        @DisplayName("SWR-043: removeTenantMembership removes membership")
        void removeTenantMembership() {
            var profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");
            var tenantId = TenantId.generate();
            profile.addTenantMembership(
                    new de.tomsblog.usermanagement.domain.model.TenantMembership(tenantId, Role.AUTHOR));
            when(repository.findByOidcSubject("sub-1")).thenReturn(Optional.of(profile));

            service.removeTenantMembership("sub-1", tenantId);

            assertThat(profile.getTenantMemberships()).isEmpty();
            verify(repository).save(profile);
        }
    }

    @Nested
    @DisplayName("listByTenantId")
    class ListByTenantId {

        @Test
        @DisplayName("SWR-051: returns users for given tenant")
        void returnsUsersForTenant() {
            var p1 = UserProfile.createFromOidc("sub-1", "a@b.com", "User1");
            var p2 = UserProfile.createInternal("admin", "hash", "admin@b.com", "Admin");
            when(repository.findByTenantId(TENANT_ID)).thenReturn(List.of(p1, p2));

            var result = service.listByTenantId(TENANT_ID);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("SWR-051: returns empty list when no users in tenant")
        void returnsEmptyList() {
            when(repository.findByTenantId(TENANT_ID)).thenReturn(List.of());

            var result = service.listByTenantId(TENANT_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("SWR-059: registers new user with hashed password")
        void registersNewUser() {
            when(repository.existsByUsername("newuser")).thenReturn(false);
            when(repository.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("securePassword1")).thenReturn("$2a$hashed");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(tenantSettingsRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());

            var command = new RegisterUserCommand("newuser", "securePassword1", "new@example.com", "New", TENANT_ID);
            var result = service.register(command);

            assertThat(result.getUsername()).isEqualTo("newuser");
            assertThat(result.getPasswordHash()).isEqualTo("$2a$hashed");
            assertThat(result.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
            verify(passwordEncoder).encode("securePassword1");
            verify(repository).save(any(UserProfile.class));
            verify(auditLogger).log(any());
        }

        @Test
        @DisplayName("SWR-059: throws when username already exists")
        void throwsWhenUsernameExists() {
            when(repository.existsByUsername("existing")).thenReturn(true);

            var command = new RegisterUserCommand("existing", "securePassword1", "new@example.com", "User", TENANT_ID);

            assertThatThrownBy(() -> service.register(command))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Username already taken");
        }

        @Test
        @DisplayName("SWR-059: throws when email already exists")
        void throwsWhenEmailExists() {
            when(repository.existsByUsername("newuser")).thenReturn(false);
            when(repository.existsByEmail("existing@example.com")).thenReturn(true);

            var command =
                    new RegisterUserCommand("newuser", "securePassword1", "existing@example.com", "User", TENANT_ID);

            assertThatThrownBy(() -> service.register(command))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Email already registered");
        }

        @Test
        @DisplayName("SWR-059: applies auto-approval when email domain matches")
        void appliesAutoApproval() {
            when(repository.existsByUsername("newuser")).thenReturn(false);
            when(repository.existsByEmail("user@company.com")).thenReturn(false);
            when(passwordEncoder.encode("securePassword1")).thenReturn("$2a$hashed");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            var settings = de.tomsblog.usermanagement.domain.model.TenantSettings.reconstitute(
                    TENANT_ID,
                    de.tomsblog.usermanagement.domain.model.LoginMode.BOTH,
                    false,
                    java.util.Set.of("company.com"),
                    "Toms Blog",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    Map.of());
            when(tenantSettingsRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));

            var command = new RegisterUserCommand("newuser", "securePassword1", "user@company.com", "User", TENANT_ID);
            var result = service.register(command);

            assertThat(result.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("SWR-093: deletes OIDC user successfully")
        void deletesOidcUserSuccessfully() {
            var profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User");
            when(repository.findByOidcSubject("sub-1")).thenReturn(Optional.of(profile));

            service.deleteUser("sub-1", TENANT_ID, "admin-user");

            verify(repository).delete(profile);
            verify(auditLogger).log(any());
        }

        @Test
        @DisplayName("SWR-093: deletes internal user successfully")
        void deletesInternalUserSuccessfully() {
            var profile = UserProfile.createInternal("testuser", "hash", "a@b.com", "User");
            when(repository.findByOidcSubject("testuser")).thenReturn(Optional.empty());
            when(repository.findByUsername("testuser")).thenReturn(Optional.of(profile));

            service.deleteUser("testuser", TENANT_ID, "admin-user");

            verify(repository).delete(profile);
            verify(auditLogger).log(any());
        }

        @Test
        @DisplayName("SWR-093: prevents self-deletion for OIDC user")
        void preventsSelfDeletionOidc() {
            var profile = UserProfile.createFromOidc("sub-self", "a@b.com", "User");
            when(repository.findByOidcSubject("sub-self")).thenReturn(Optional.of(profile));

            assertThatThrownBy(() -> service.deleteUser("sub-self", TENANT_ID, "sub-self"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot delete your own account");
        }

        @Test
        @DisplayName("SWR-093: prevents self-deletion for internal user")
        void preventsSelfDeletionInternal() {
            var profile = UserProfile.createInternal("admin", "hash", "a@b.com", "Admin");
            when(repository.findByOidcSubject("admin")).thenReturn(Optional.empty());
            when(repository.findByUsername("admin")).thenReturn(Optional.of(profile));

            assertThatThrownBy(() -> service.deleteUser("admin", TENANT_ID, "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot delete your own account");
        }

        @Test
        @DisplayName("SWR-093: prevents deletion of SUPERADMIN")
        void preventsSuperadminDeletion() {
            var profile = UserProfile.createFromOidc("sub-super", "super@b.com", "Super");
            profile.assignGlobalRole(Role.SUPERADMIN);
            when(repository.findByOidcSubject("sub-super")).thenReturn(Optional.of(profile));

            assertThatThrownBy(() -> service.deleteUser("sub-super", TENANT_ID, "other-user"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot delete the SUPERADMIN account");
        }

        @Test
        @DisplayName("SWR-093: throws when user not found")
        void throwsWhenUserNotFound() {
            when(repository.findByOidcSubject("unknown")).thenReturn(Optional.empty());
            when(repository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteUser("unknown", TENANT_ID, "admin"))
                    .isInstanceOf(UserProfileNotFoundException.class);
        }

        @Test
        @DisplayName("SWR-093: deletes user when profileIdentifier is null (both oidcSubject and username null)")
        void deletesWhenProfileIdentifierIsNull() {
            var profile = mock(UserProfile.class);
            when(profile.getOidcSubject()).thenReturn(null);
            when(profile.getUsername()).thenReturn(null);
            when(profile.getGlobalRoles()).thenReturn(java.util.Set.of(Role.READER));
            when(profile.getId())
                    .thenReturn(new de.tomsblog.usermanagement.domain.model.UserProfileId(java.util.UUID.randomUUID()));
            when(repository.findByOidcSubject("identifier")).thenReturn(Optional.of(profile));

            service.deleteUser("identifier", TENANT_ID, "admin-user");

            verify(repository).delete(profile);
        }

        @Test
        @DisplayName("SWR-093: deletes user when globalRoles is null")
        void deletesWhenGlobalRolesIsNull() {
            var profile = mock(UserProfile.class);
            when(profile.getOidcSubject()).thenReturn("sub-other");
            when(profile.getGlobalRoles()).thenReturn(null);
            when(profile.getId())
                    .thenReturn(new de.tomsblog.usermanagement.domain.model.UserProfileId(java.util.UUID.randomUUID()));
            when(repository.findByOidcSubject("sub-other")).thenReturn(Optional.of(profile));

            service.deleteUser("sub-other", TENANT_ID, "admin-user");

            verify(repository).delete(profile);
        }
    }

    @Nested
    @DisplayName("resolveDefaultRole")
    class ResolveDefaultRole {

        @Test
        @DisplayName("SWR-094: uses configured default role from tenant settings")
        void usesConfiguredDefaultRole() {
            when(repository.findByOidcSubject("sub-new")).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            var settings = de.tomsblog.usermanagement.domain.model.TenantSettings.reconstitute(
                    TENANT_ID,
                    de.tomsblog.usermanagement.domain.model.LoginMode.BOTH,
                    false,
                    java.util.Set.of(),
                    "Blog",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "AUTHOR",
                    null,
                    null,
                    false,
                    Map.of());
            when(tenantSettingsRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));

            var command = new SyncOidcUserCommand("sub-new", "user@example.com", "User", List.of(), TENANT_ID);
            var result = service.syncFromOidc(command);

            assertThat(result.getTenantMemberships()).anyMatch(m -> m.role() == Role.AUTHOR);
        }

        @Test
        @DisplayName("SWR-094: falls back to READER when defaultRole is null")
        void fallsBackToReaderWhenNull() {
            when(repository.findByOidcSubject("sub-null")).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            var settings = de.tomsblog.usermanagement.domain.model.TenantSettings.reconstitute(
                    TENANT_ID,
                    de.tomsblog.usermanagement.domain.model.LoginMode.BOTH,
                    false,
                    java.util.Set.of(),
                    "Blog",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    Map.of());
            when(tenantSettingsRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));

            var command = new SyncOidcUserCommand("sub-null", "user@example.com", "User", List.of(), TENANT_ID);
            var result = service.syncFromOidc(command);

            assertThat(result.getTenantMemberships()).anyMatch(m -> m.role() == Role.READER);
        }

        @Test
        @DisplayName("SWR-094: falls back to READER when defaultRole is invalid")
        void fallsBackToReaderWhenInvalid() {
            when(repository.findByOidcSubject("sub-inv")).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            var settings = de.tomsblog.usermanagement.domain.model.TenantSettings.reconstitute(
                    TENANT_ID,
                    de.tomsblog.usermanagement.domain.model.LoginMode.BOTH,
                    false,
                    java.util.Set.of(),
                    "Blog",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "INVALID_ROLE",
                    null,
                    null,
                    false,
                    Map.of());
            when(tenantSettingsRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));

            var command = new SyncOidcUserCommand("sub-inv", "user@example.com", "User", List.of(), TENANT_ID);
            var result = service.syncFromOidc(command);

            assertThat(result.getTenantMemberships()).anyMatch(m -> m.role() == Role.READER);
        }
    }
}
