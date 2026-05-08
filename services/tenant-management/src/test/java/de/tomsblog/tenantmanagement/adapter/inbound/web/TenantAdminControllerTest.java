package de.tomsblog.tenantmanagement.adapter.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.tenantmanagement.application.port.inbound.TenantManagementUseCase;
import de.tomsblog.tenantmanagement.domain.model.Tenant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

class TenantAdminControllerTest {

    private TenantManagementUseCase useCase;
    private TenantAdminController controller;

    @BeforeEach
    void setUp() {
        useCase = mock(TenantManagementUseCase.class);
        controller = new TenantAdminController(useCase);
    }

    @Test
    @DisplayName("SWR-073: loginPage returns login view")
    void loginPage() {
        assertThat(controller.loginPage()).isEqualTo("admin-login");
    }

    @Test
    @DisplayName("SWR-073: listTenants returns tenants view")
    void listTenants() {
        var t = Tenant.create(TenantId.generate(), "s", "Name");
        when(useCase.listAllTenants()).thenReturn(List.of(t));
        var model = new ExtendedModelMap();

        var view = controller.listTenants(model);

        assertThat(view).isEqualTo("admin/tenants");
        assertThat(model.get("tenants")).isNotNull();
    }

    @Test
    @DisplayName("SWR-073: listTenants handles exception gracefully")
    void listTenantsException() {
        when(useCase.listAllTenants()).thenThrow(new RuntimeException("db error"));
        var model = new ExtendedModelMap();

        var view = controller.listTenants(model);

        assertThat(view).isEqualTo("admin/tenants");
        assertThat((List<?>) model.get("tenants")).isEmpty();
    }

    @Test
    @DisplayName("SWR-073: createTenant delegates and redirects")
    void createTenant() {
        when(useCase.createTenant(any(), any())).thenReturn(Tenant.create(TenantId.generate(), "s", "N"));

        var view = controller.createTenant("my-blog", "My Blog");

        assertThat(view).isEqualTo("redirect:/tenant/admin/tenants");
        verify(useCase).createTenant("my-blog", "My Blog");
    }

    @Test
    @DisplayName("SWR-073: showSettings loads tenant and returns settings view")
    void showSettings() {
        var tenantId = UUID.randomUUID();
        var tenant = Tenant.create(new TenantId(tenantId), "s", "Name");
        when(useCase.getTenant(any())).thenReturn(tenant);
        var model = new ExtendedModelMap();

        var view = controller.showSettings(tenantId, new MockHttpSession(), "general", model);

        assertThat(view).isEqualTo("admin/settings");
        assertThat(model.get("tenant")).isNotNull();
        assertThat(model.get("activeTab")).isEqualTo("general");
    }

    @Test
    @DisplayName("SWR-073: showSettings handles exception gracefully")
    void showSettingsException() {
        when(useCase.getTenant(any())).thenThrow(new RuntimeException("not found"));
        var model = new ExtendedModelMap();

        var view = controller.showSettings(UUID.randomUUID(), new MockHttpSession(), "oidc", model);

        assertThat(view).isEqualTo("admin/settings");
        assertThat(model.get("activeTab")).isEqualTo("oidc");
    }

    @Test
    @DisplayName("SWR-073: updateGeneralSettings delegates and redirects")
    void updateGeneralSettings() {
        var tenantId = UUID.randomUUID();
        when(useCase.updateGeneralSettings(any(), any(), any(), any(), any(boolean.class), any()))
                .thenReturn(Tenant.create(new TenantId(tenantId), "s", "N"));

        var view = controller.updateGeneralSettings(
                tenantId, new MockHttpSession(), "INTERNAL", false, "example.com, test.org", "Name", "Tag");

        assertThat(view).isEqualTo("redirect:/tenant/admin/settings?tab=general");
        verify(useCase).updateGeneralSettings(any(), any(), any(), any(), any(boolean.class), any());
    }

    @Test
    @DisplayName("SWR-073: updateGeneralSettings handles blank domains")
    void updateGeneralSettingsBlankDomains() {
        var tenantId = UUID.randomUUID();
        when(useCase.updateGeneralSettings(any(), any(), any(), any(), any(boolean.class), any()))
                .thenReturn(Tenant.create(new TenantId(tenantId), "s", "N"));

        var view =
                controller.updateGeneralSettings(tenantId, new MockHttpSession(), "INTERNAL", false, "", "Name", null);

        assertThat(view).isEqualTo("redirect:/tenant/admin/settings?tab=general");
    }

    @Test
    @DisplayName("SWR-073: updateOidcSettings delegates and redirects")
    void updateOidcSettings() {
        var tenantId = UUID.randomUUID();
        when(useCase.updateOidcSettings(any(), any(), any(), any()))
                .thenReturn(Tenant.create(new TenantId(tenantId), "s", "N"));

        var view = controller.updateOidcSettings(tenantId, new MockHttpSession(), "https://iss", "cid", "cs");

        assertThat(view).isEqualTo("redirect:/tenant/admin/settings?tab=oidc");
    }

    @Test
    @DisplayName("SWR-073: updateLegalSettings delegates and redirects")
    void updateLegalSettings() {
        var tenantId = UUID.randomUUID();
        when(useCase.updateLegalSettings(any(), any(), any()))
                .thenReturn(Tenant.create(new TenantId(tenantId), "s", "N"));

        var view = controller.updateLegalSettings(tenantId, new MockHttpSession(), "imp", "priv");

        assertThat(view).isEqualTo("redirect:/tenant/admin/settings?tab=legal");
    }

    @Test
    @DisplayName("SWR-073: switchTenant sets session and redirects to referer")
    void switchTenantWithReferer() {
        var session = new MockHttpSession();
        var request = new MockHttpServletRequest();
        request.addHeader("Referer", "/tenant/admin/settings");
        var tid = UUID.randomUUID();

        var view = controller.switchTenant(tid, session, request);

        assertThat(view).isEqualTo("redirect:/tenant/admin/settings");
        assertThat(session.getAttribute("activeTenantId")).isEqualTo(tid.toString());
    }

    @Test
    @DisplayName("SWR-073: switchTenant redirects to tenants when no referer")
    void switchTenantWithoutReferer() {
        var session = new MockHttpSession();
        var request = new MockHttpServletRequest();
        var tid = UUID.randomUUID();

        var view = controller.switchTenant(tid, session, request);

        assertThat(view).isEqualTo("redirect:/tenant/admin/tenants");
    }

    @Test
    @DisplayName("SWR-073: resolveActiveTenant prefers session over header")
    void resolveActiveTenantSession() {
        var headerTenant = UUID.randomUUID();
        var sessionTenant = UUID.randomUUID();
        var session = new MockHttpSession();
        session.setAttribute("activeTenantId", sessionTenant.toString());
        var tenant = Tenant.create(new TenantId(sessionTenant), "s", "N");
        when(useCase.getTenant(new TenantId(sessionTenant))).thenReturn(tenant);
        var model = new ExtendedModelMap();

        controller.showSettings(headerTenant, session, "general", model);

        verify(useCase).getTenant(new TenantId(sessionTenant));
    }

    @Test
    @DisplayName("SWR-073: resolveActiveTenant falls back to header on invalid session value")
    void resolveActiveTenantInvalidSession() {
        var headerTenant = UUID.randomUUID();
        var session = new MockHttpSession();
        session.setAttribute("activeTenantId", "not-a-uuid");
        var tenant = Tenant.create(new TenantId(headerTenant), "s", "N");
        when(useCase.getTenant(new TenantId(headerTenant))).thenReturn(tenant);
        var model = new ExtendedModelMap();

        controller.showSettings(headerTenant, session, "general", model);

        verify(useCase).getTenant(new TenantId(headerTenant));
    }
}
