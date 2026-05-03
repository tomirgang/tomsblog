package de.tomsblog.usermanagement.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.outbound.TenantSettingsRepository;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
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

    private TenantSettingsService service;

    private static final TenantId TENANT_ID = TenantId.generate();

    @BeforeEach
    void setUp() {
        service = new TenantSettingsService(repository);
    }

    @Nested
    @DisplayName("getSettings")
    class GetSettings {

        @Test
        @DisplayName("SWR-044: returns existing settings when found")
        void returnsExistingSettings() {
            var settings = TenantSettings.reconstitute(TENANT_ID, LoginMode.OIDC, true, Set.of("test.com"));
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

            assertThat(result.getLoginMode()).isEqualTo(LoginMode.BOTH);
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
}
