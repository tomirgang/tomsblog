package de.tomsblog.usermanagement.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.SyncInternalUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.SyncOidcUserCommand;
import de.tomsblog.usermanagement.application.port.outbound.TenantSettingsRepository;
import de.tomsblog.usermanagement.application.port.outbound.UserProfileRepository;
import de.tomsblog.usermanagement.domain.model.ApprovalStatus;
import de.tomsblog.usermanagement.domain.model.Role;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository repository;

    @Mock
    private TenantSettingsRepository tenantSettingsRepository;

    private UserProfileService service;

    private static final TenantId TENANT_ID = TenantId.generate();

    @BeforeEach
    void setUp() {
        service = new UserProfileService(repository, tenantSettingsRepository);
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
                    TENANT_ID, de.tomsblog.usermanagement.domain.model.LoginMode.BOTH, true, java.util.Set.of());
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
                    java.util.Set.of("company.com"));
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
}
