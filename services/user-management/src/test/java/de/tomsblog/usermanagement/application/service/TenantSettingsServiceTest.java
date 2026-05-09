package de.tomsblog.usermanagement.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.tomsblog.shared.audit.AuditLogger;
import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.outbound.TenantSettingsRepository;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantSettingsServiceTest {

    @Mock
    private TenantSettingsRepository repository;

    @Mock
    private AuditLogger auditLogger;

    private TenantSettingsService service;

    private static final TenantId TENANT_ID = TenantId.generate();

    @BeforeEach
    void setUp() {
        service = new TenantSettingsService(repository, auditLogger);
    }

    @Nested
    @DisplayName("getSettings")
    class GetSettings {

        @Test
        @DisplayName("SWR-044: returns existing settings when found")
        void returnsExistingSettings() {
            var settings = TenantSettings.reconstitute(
                    TENANT_ID,
                    LoginMode.OIDC,
                    true,
                    Set.of("test.com"),
                    "Toms Blog",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));

            var result = service.getSettings(TENANT_ID);

            assertThat(result.getLoginMode()).isEqualTo(LoginMode.OIDC);
            assertThat(result.isAutoApproveOidc()).isTrue();
        }

        @Test
        @DisplayName("SWR-044: creates and saves default settings when not found")
        void createsDefaultWhenNotFound() {
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.getSettings(TENANT_ID);

            assertThat(result.getLoginMode()).isEqualTo(LoginMode.INTERNAL);
            assertThat(result.isAutoApproveOidc()).isFalse();
            verify(repository).save(any(TenantSettings.class));
        }
    }

    @Nested
    @DisplayName("updateLoginMode")
    class UpdateLoginMode {

        @Test
        @DisplayName("SWR-044: updates login mode on existing settings")
        void updatesExistingSettings() {
            var settings = TenantSettings.create(TENANT_ID);
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.updateLoginMode(TENANT_ID, LoginMode.INTERNAL);

            assertThat(result.getLoginMode()).isEqualTo(LoginMode.INTERNAL);
            verify(repository).save(settings);
        }

        @Test
        @DisplayName("SWR-044: creates default then updates when not found")
        void createsDefaultThenUpdates() {
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.updateLoginMode(TENANT_ID, LoginMode.OIDC);

            assertThat(result.getLoginMode()).isEqualTo(LoginMode.OIDC);
            verify(repository, times(2)).save(any(TenantSettings.class));
        }
    }

    @Nested
    @DisplayName("updateAutoApproval")
    class UpdateAutoApproval {

        @Test
        @DisplayName("SWR-045: updates auto-approval settings")
        void updatesAutoApproval() {
            var settings = TenantSettings.create(TENANT_ID);
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.updateAutoApproval(TENANT_ID, true, Set.of("company.com", "corp.net"));

            assertThat(result.isAutoApproveOidc()).isTrue();
            assertThat(result.getAutoApproveEmailDomains()).containsExactlyInAnyOrder("company.com", "corp.net");
            verify(repository).save(settings);
        }

        @Test
        @DisplayName("SWR-045: creates default then updates when not found")
        void createsDefaultThenUpdates() {
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.updateAutoApproval(TENANT_ID, true, Set.of("example.org"));

            assertThat(result.isAutoApproveOidc()).isTrue();
            assertThat(result.getAutoApproveEmailDomains()).containsExactly("example.org");
            verify(repository, times(2)).save(any(TenantSettings.class));
        }
    }

    @Nested
    @DisplayName("updateSettings")
    class UpdateSettings {

        @Test
        @DisplayName("SWR-052: updates all settings including branding")
        void updatesAllSettings() {
            var settings = TenantSettings.create(TENANT_ID);
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.updateSettings(
                    TENANT_ID,
                    LoginMode.OIDC,
                    true,
                    Set.of("test.com"),
                    "My Blog",
                    "A tagline",
                    null,
                    null,
                    "https://auth.example.com",
                    "my-client-id",
                    "my-secret");

            assertThat(result.getLoginMode()).isEqualTo(LoginMode.OIDC);
            assertThat(result.isAutoApproveOidc()).isTrue();
            assertThat(result.getAutoApproveEmailDomains()).containsExactly("test.com");
            assertThat(result.getDisplayName()).isEqualTo("My Blog");
            assertThat(result.getTagline()).isEqualTo("A tagline");
            verify(repository).save(settings);
        }

        @Test
        @DisplayName("SWR-052: creates default then updates when not found")
        void createsDefaultThenUpdates() {
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.updateSettings(
                    TENANT_ID, LoginMode.INTERNAL, false, Set.of(), "New Blog", null, null, null, null, null, null);

            assertThat(result.getLoginMode()).isEqualTo(LoginMode.INTERNAL);
            assertThat(result.getDisplayName()).isEqualTo("New Blog");
            assertThat(result.getTagline()).isNull();
            verify(repository, times(2)).save(any(TenantSettings.class));
        }

        @Test
        @DisplayName("SWR-061: preserves existing secret when input is masked")
        void preservesSecretWhenMasked() {
            var settings = TenantSettings.create(TENANT_ID);
            settings.updateOidcClientSecret("existing-secret");
            settings.updateOidcIssuerUrl("https://auth.example.com");
            settings.updateOidcClientId("client-id");
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.updateSettings(
                    TENANT_ID,
                    LoginMode.OIDC,
                    false,
                    Set.of(),
                    "Blog",
                    null,
                    null,
                    null,
                    "https://auth.example.com",
                    "client-id",
                    "***");

            assertThat(result.getOidcClientSecret()).isEqualTo("existing-secret");
        }

        @Test
        @DisplayName("SWR-061: preserves existing secret when input is empty")
        void preservesSecretWhenEmpty() {
            var settings = TenantSettings.create(TENANT_ID);
            settings.updateOidcClientSecret("existing-secret");
            settings.updateOidcIssuerUrl("https://auth.example.com");
            settings.updateOidcClientId("client-id");
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.updateSettings(
                    TENANT_ID,
                    LoginMode.OIDC,
                    false,
                    Set.of(),
                    "Blog",
                    null,
                    null,
                    null,
                    "https://auth.example.com",
                    "client-id",
                    "");

            assertThat(result.getOidcClientSecret()).isEqualTo("existing-secret");
        }

        @Test
        @DisplayName("SWR-061: validates OIDC config for BOTH mode")
        void validatesOidcForBothMode() {
            var settings = TenantSettings.create(TENANT_ID);
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));

            assertThatThrownBy(() -> service.updateSettings(
                            TENANT_ID, LoginMode.BOTH, false, Set.of(), "Blog", null, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("oidcIssuerUrl is required");
        }

        @Test
        @DisplayName("SWR-061: validates clientId for OIDC mode")
        void validatesClientIdForOidcMode() {
            var settings = TenantSettings.create(TENANT_ID);
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));

            assertThatThrownBy(() -> service.updateSettings(
                            TENANT_ID,
                            LoginMode.OIDC,
                            false,
                            Set.of(),
                            "Blog",
                            null,
                            null,
                            null,
                            "https://auth.example.com",
                            null,
                            null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("oidcClientId is required");
        }

        @Test
        @DisplayName("SWR-061: validates blank clientId for OIDC mode")
        void validatesBlankClientIdForOidcMode() {
            var settings = TenantSettings.create(TENANT_ID);
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));

            assertThatThrownBy(() -> service.updateSettings(
                            TENANT_ID,
                            LoginMode.OIDC,
                            false,
                            Set.of(),
                            "Blog",
                            null,
                            null,
                            null,
                            "https://auth.example.com",
                            "  ",
                            null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("oidcClientId is required");
        }
    }

    @Nested
    @DisplayName("listAllTenants")
    class ListAllTenants {

        @Test
        @DisplayName("SWR-053: lists all tenant settings")
        void listsAllTenants() {
            var s1 = TenantSettings.create(TENANT_ID);
            var s2 = TenantSettings.create(TenantId.generate());
            when(repository.findAll()).thenReturn(List.of(s1, s2));

            var result = service.listAllTenants();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("SWR-053: returns empty list when no tenants")
        void returnsEmptyList() {
            when(repository.findAll()).thenReturn(List.of());

            var result = service.listAllTenants();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateGeneralSettings")
    class UpdateGeneralSettings {

        @Test
        @DisplayName("SWR-071: updates only general settings fields")
        void updatesGeneralSettings() {
            var settings = TenantSettings.create(TENANT_ID);
            settings.updateImpressumContent("Existing Impressum");
            settings.updateOidcIssuerUrl("https://auth.example.com");
            settings.updateOidcClientId("client-id");
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.updateGeneralSettings(
                    TENANT_ID, "New Blog", "New Tagline", LoginMode.INTERNAL, true, Set.of("test.com"));

            assertThat(result.getDisplayName()).isEqualTo("New Blog");
            assertThat(result.getTagline()).isEqualTo("New Tagline");
            assertThat(result.getLoginMode()).isEqualTo(LoginMode.INTERNAL);
            assertThat(result.isAutoApproveOidc()).isTrue();
            assertThat(result.getAutoApproveEmailDomains()).containsExactly("test.com");
            assertThat(result.getImpressumContent()).isEqualTo("Existing Impressum");
            verify(repository).save(settings);
            verify(auditLogger).log(any());
        }

        @Test
        @DisplayName("SWR-071: validates OIDC config when switching to OIDC mode")
        void validatesOidcOnModeSwitch() {
            var settings = TenantSettings.create(TENANT_ID);
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));

            assertThatThrownBy(() ->
                            service.updateGeneralSettings(TENANT_ID, "Blog", null, LoginMode.OIDC, false, Set.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("oidcIssuerUrl is required");
        }
    }

    @Nested
    @DisplayName("updateOidcSettings")
    class UpdateOidcSettings {

        @Test
        @DisplayName("SWR-071: updates only OIDC settings fields")
        void updatesOidcSettings() {
            var settings = TenantSettings.create(TENANT_ID);
            settings.updateDisplayName("My Blog");
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result =
                    service.updateOidcSettings(TENANT_ID, "https://auth.example.com", "new-client-id", "new-secret");

            assertThat(result.getOidcIssuerUrl()).isEqualTo("https://auth.example.com");
            assertThat(result.getOidcClientId()).isEqualTo("new-client-id");
            assertThat(result.getOidcClientSecret()).isEqualTo("new-secret");
            assertThat(result.getDisplayName()).isEqualTo("My Blog");
            verify(repository).save(settings);
            verify(auditLogger).log(any());
        }

        @Test
        @DisplayName("SWR-071: preserves existing secret when input is masked")
        void preservesSecretWhenMasked() {
            var settings = TenantSettings.create(TENANT_ID);
            settings.updateOidcClientSecret("existing-secret");
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.updateOidcSettings(TENANT_ID, "https://auth.example.com", "client-id", "***");

            assertThat(result.getOidcClientSecret()).isEqualTo("existing-secret");
        }

        @Test
        @DisplayName("SWR-071: preserves existing secret when input is empty")
        void preservesSecretWhenEmpty() {
            var settings = TenantSettings.create(TENANT_ID);
            settings.updateOidcClientSecret("existing-secret");
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.updateOidcSettings(TENANT_ID, "https://auth.example.com", "client-id", "");

            assertThat(result.getOidcClientSecret()).isEqualTo("existing-secret");
        }

        @Test
        @DisplayName("SWR-071: preserves existing secret when input is null")
        void preservesSecretWhenNull() {
            var settings = TenantSettings.create(TENANT_ID);
            settings.updateOidcClientSecret("existing-secret");
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.updateOidcSettings(TENANT_ID, "https://auth.example.com", "client-id", null);

            assertThat(result.getOidcClientSecret()).isEqualTo("existing-secret");
        }
    }

    @Nested
    @DisplayName("updateLegalSettings")
    class UpdateLegalSettings {

        @Test
        @DisplayName("SWR-071: updates only legal settings fields")
        void updatesLegalSettings() {
            var settings = TenantSettings.create(TENANT_ID);
            settings.updateDisplayName("My Blog");
            settings.updateOidcIssuerUrl("https://auth.example.com");
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.updateLegalSettings(TENANT_ID, "New Impressum", "New Privacy");

            assertThat(result.getImpressumContent()).isEqualTo("New Impressum");
            assertThat(result.getPrivacyPolicyContent()).isEqualTo("New Privacy");
            assertThat(result.getDisplayName()).isEqualTo("My Blog");
            assertThat(result.getOidcIssuerUrl()).isEqualTo("https://auth.example.com");
            verify(repository).save(settings);
            verify(auditLogger).log(any());
        }

        @Test
        @DisplayName("SWR-071: allows clearing legal content")
        void allowsClearingLegalContent() {
            var settings = TenantSettings.create(TENANT_ID);
            settings.updateImpressumContent("Old Impressum");
            when(repository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(settings));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.updateLegalSettings(TENANT_ID, null, null);

            assertThat(result.getImpressumContent()).isNull();
            assertThat(result.getPrivacyPolicyContent()).isNull();
        }
    }
}
