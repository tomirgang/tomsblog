package de.tomsblog.usermanagement.adapter.inbound.web;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.application.port.inbound.UserProfileUseCase;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.Role;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

@DisplayName("SWR-051: AuthAdminController")
class AuthAdminControllerTest {

    private static final UUID TENANT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_TENANT = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final TenantId TENANT_ID = TenantId.of(TENANT_UUID);

    private UserProfileUseCase userProfileUseCase;
    private TenantSettingsUseCase tenantSettingsUseCase;
    private AuthAdminController controller;

    @BeforeEach
    void setUp() {
        userProfileUseCase = mock(UserProfileUseCase.class);
        tenantSettingsUseCase = mock(TenantSettingsUseCase.class);
        controller = new AuthAdminController(userProfileUseCase, tenantSettingsUseCase);
    }

    @Test
    @DisplayName("listUsers returns users for the tenant")
    void listUsers() {
        var user = UserProfile.createFromOidc("sub-1", "user@test.com", "User");
        when(userProfileUseCase.listByTenantId(any())).thenReturn(List.of(user));

        Model model = new ConcurrentModel();
        MockHttpSession session = new MockHttpSession();
        String view = controller.listUsers(TENANT_UUID, session, model);

        assertThat(view).isEqualTo("admin/users");
        assertThat(((List<?>) model.getAttribute("users"))).hasSize(1);
    }

    @Test
    @DisplayName("listUsers returns empty list on error")
    void listUsersError() {
        when(userProfileUseCase.listByTenantId(any())).thenThrow(new RuntimeException("DB error"));

        Model model = new ConcurrentModel();
        String view = controller.listUsers(TENANT_UUID, new MockHttpSession(), model);

        assertThat(view).isEqualTo("admin/users");
        assertThat(((List<?>) model.getAttribute("users"))).isEmpty();
    }

    @Test
    @DisplayName("approveUser delegates and redirects")
    void approveUser() {
        String view = controller.approveUser("user-1");
        verify(userProfileUseCase).approveUser("user-1");
        assertThat(view).isEqualTo("redirect:/auth/admin/users");
    }

    @Test
    @DisplayName("rejectUser delegates and redirects")
    void rejectUser() {
        String view = controller.rejectUser("user-1");
        verify(userProfileUseCase).rejectUser("user-1");
        assertThat(view).isEqualTo("redirect:/auth/admin/users");
    }

    @Test
    @DisplayName("changeRole delegates with correct tenant and role")
    void changeRole() {
        MockHttpSession session = new MockHttpSession();
        String view = controller.changeRole("user-1", "AUTHOR", TENANT_UUID, session);

        verify(userProfileUseCase).addTenantMembership("user-1", TENANT_ID, Role.AUTHOR);
        assertThat(view).isEqualTo("redirect:/auth/admin/users");
    }

    @Test
    @DisplayName("showSettings returns settings view with data")
    void showSettings() {
        var settings = TenantSettings.reconstitute(
                TENANT_ID, LoginMode.BOTH, true, Set.of("test.com"), "Blog", null, null, null, null, null, null);
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        Model model = new ConcurrentModel();
        String view = controller.showSettings(TENANT_UUID, new MockHttpSession(), "general", model);

        assertThat(view).isEqualTo("admin/settings");
        assertThat(model.getAttribute("settings")).isNotNull();
        assertThat(model.getAttribute("activeTab")).isEqualTo("general");
    }

    @Test
    @DisplayName("showSettings handles error gracefully")
    void showSettingsError() {
        when(tenantSettingsUseCase.getSettings(any())).thenThrow(new RuntimeException("fail"));

        Model model = new ConcurrentModel();
        String view = controller.showSettings(TENANT_UUID, new MockHttpSession(), "general", model);

        assertThat(view).isEqualTo("admin/settings");
        assertThat(model.getAttribute("settings")).isNull();
    }

    @Test
    @DisplayName("updateGeneralSettings delegates and redirects")
    void updateGeneralSettings() {
        MockHttpSession session = new MockHttpSession();
        String view = controller.updateGeneralSettings(
                TENANT_UUID, session, "BOTH", true, "example.com, test.org", "My Blog", "Tagline");

        verify(tenantSettingsUseCase)
                .updateGeneralSettings(
                        eq(TENANT_ID),
                        eq("My Blog"),
                        eq("Tagline"),
                        eq(LoginMode.BOTH),
                        eq(true),
                        eq(Set.of("example.com", "test.org")));
        assertThat(view).isEqualTo("redirect:/auth/admin/settings?tab=general");
    }

    @Test
    @DisplayName("updateGeneralSettings with blank domains creates empty set")
    void updateGeneralSettingsEmptyDomains() {
        MockHttpSession session = new MockHttpSession();
        controller.updateGeneralSettings(TENANT_UUID, session, "OIDC", false, "  ", "Blog", null);

        verify(tenantSettingsUseCase)
                .updateGeneralSettings(eq(TENANT_ID), eq("Blog"), any(), eq(LoginMode.OIDC), eq(false), eq(Set.of()));
    }

    @Test
    @DisplayName("updateOidcSettings delegates and redirects")
    void updateOidcSettings() {
        MockHttpSession session = new MockHttpSession();
        String view = controller.updateOidcSettings(
                TENANT_UUID, session, "https://auth.example.com", "client-id", "client-secret");

        verify(tenantSettingsUseCase)
                .updateOidcSettings(
                        eq(TENANT_ID), eq("https://auth.example.com"), eq("client-id"), eq("client-secret"));
        assertThat(view).isEqualTo("redirect:/auth/admin/settings?tab=oidc");
    }

    @Test
    @DisplayName("updateLegalSettings delegates and redirects")
    void updateLegalSettings() {
        MockHttpSession session = new MockHttpSession();
        String view = controller.updateLegalSettings(TENANT_UUID, session, "Impressum text", "Privacy text");

        verify(tenantSettingsUseCase).updateLegalSettings(eq(TENANT_ID), eq("Impressum text"), eq("Privacy text"));
        assertThat(view).isEqualTo("redirect:/auth/admin/settings?tab=legal");
    }

    @Test
    @DisplayName("switchTenant stores tenant in session and redirects to referer")
    void switchTenant() {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Referer", "/auth/admin/settings");

        String view = controller.switchTenant(OTHER_TENANT, session, request);

        assertThat(session.getAttribute("activeTenantId")).isEqualTo(OTHER_TENANT.toString());
        assertThat(view).isEqualTo("redirect:/auth/admin/settings");
    }

    @Test
    @DisplayName("switchTenant redirects to / when no referer")
    void switchTenantNoReferer() {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest();

        String view = controller.switchTenant(OTHER_TENANT, session, request);

        assertThat(view).isEqualTo("redirect:/");
    }

    @Test
    @DisplayName("uses session tenant override when present")
    void sessionTenantOverride() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("activeTenantId", OTHER_TENANT.toString());

        when(userProfileUseCase.listByTenantId(any())).thenReturn(List.of());

        Model model = new ConcurrentModel();
        controller.listUsers(TENANT_UUID, session, model);

        verify(userProfileUseCase).listByTenantId(TenantId.of(OTHER_TENANT));
    }

    @Test
    @DisplayName("ignores invalid session tenant")
    void invalidSessionTenant() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("activeTenantId", "not-a-uuid");

        when(userProfileUseCase.listByTenantId(any())).thenReturn(List.of());

        Model model = new ConcurrentModel();
        controller.listUsers(TENANT_UUID, session, model);

        verify(userProfileUseCase).listByTenantId(TENANT_ID);
    }
}
