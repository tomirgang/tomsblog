package de.tomsblog.usermanagement.adapter.inbound.web;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

@DisplayName("SWR-016: AuthLoginController")
class AuthLoginControllerTest {

    private static final UUID TENANT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private TenantSettingsUseCase tenantSettingsUseCase;
    private AuthLoginController controller;

    @BeforeEach
    void setUp() {
        tenantSettingsUseCase = mock(TenantSettingsUseCase.class);
        controller = new AuthLoginController(tenantSettingsUseCase);
    }

    @Test
    @DisplayName("login page returns 'login' view with loginMode from tenant settings")
    void loginReturnsViewWithLoginMode() {
        var settings = TenantSettings.reconstitute(
                TenantId.of(TENANT_UUID), LoginMode.OIDC, false, Set.of(), "Blog", null, null, null, null, null, null);
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        Model model = new ConcurrentModel();
        String view = controller.login(TENANT_UUID, model);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("loginMode")).isEqualTo("OIDC");
    }

    @Test
    @DisplayName("login page defaults to BOTH when settings are null")
    void loginDefaultsToBothWhenNull() {
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(null);

        Model model = new ConcurrentModel();
        String view = controller.login(TENANT_UUID, model);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("loginMode")).isEqualTo("BOTH");
    }

    @Test
    @DisplayName("login page defaults to BOTH when exception occurs")
    void loginDefaultsToBothOnException() {
        when(tenantSettingsUseCase.getSettings(any())).thenThrow(new RuntimeException("DB down"));

        Model model = new ConcurrentModel();
        String view = controller.login(TENANT_UUID, model);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("loginMode")).isEqualTo("BOTH");
    }

    @Test
    @DisplayName("admin login returns 'admin-login' view")
    void adminLogin() {
        String view = controller.adminLogin();
        assertThat(view).isEqualTo("admin-login");
    }
}
