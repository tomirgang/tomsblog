package de.tomsblog.tenantmanagement.adapter.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.tenantmanagement.application.port.inbound.TenantManagementUseCase;
import de.tomsblog.tenantmanagement.domain.model.Tenant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        assertThat(controller.loginPage()).isEqualTo("tenant-admin-login");
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

        assertThat(view).isEqualTo("tenant-admin/settings");
        assertThat(model.get("tenant")).isNotNull();
        assertThat(model.get("activeTab")).isEqualTo("general");
    }

    @Test
    @DisplayName("SWR-073: showSettings handles exception gracefully")
    void showSettingsException() {
        when(useCase.getTenant(any())).thenThrow(new RuntimeException("not found"));
        var model = new ExtendedModelMap();

        var view = controller.showSettings(UUID.randomUUID(), new MockHttpSession(), "oidc", model);

        assertThat(view).isEqualTo("tenant-admin/settings");
        assertThat(model.get("activeTab")).isEqualTo("oidc");
    }

    @Test
    @DisplayName("SWR-073: updateGeneralSettings delegates and redirects")
    void updateGeneralSettings() {
        var tenantId = UUID.randomUUID();
        when(useCase.updateGeneralSettings(any(), any(), any(), any(), any(boolean.class), any(), any(), any(), any()))
                .thenReturn(Tenant.create(new TenantId(tenantId), "s", "N"));

        var view = controller.updateGeneralSettings(
                tenantId,
                new MockHttpSession(),
                "INTERNAL",
                false,
                "example.com, test.org",
                "Name",
                "Tag",
                null,
                null,
                null);

        assertThat(view).isEqualTo("redirect:/tenant/admin/settings?tab=general");
        verify(useCase)
                .updateGeneralSettings(any(), any(), any(), any(), any(boolean.class), any(), any(), any(), any());
    }

    @Test
    @DisplayName("SWR-073: updateGeneralSettings handles blank domains")
    void updateGeneralSettingsBlankDomains() {
        var tenantId = UUID.randomUUID();
        when(useCase.updateGeneralSettings(any(), any(), any(), any(), any(boolean.class), any(), any(), any(), any()))
                .thenReturn(Tenant.create(new TenantId(tenantId), "s", "N"));

        var view = controller.updateGeneralSettings(
                tenantId, new MockHttpSession(), "INTERNAL", false, "", "Name", null, null, null, null);

        assertThat(view).isEqualTo("redirect:/tenant/admin/settings?tab=general");
    }

    @Test
    @DisplayName("SWR-073: updateGeneralSettings filters empty domain segments")
    void updateGeneralSettingsEmptyDomainSegments() {
        var tenantId = UUID.randomUUID();
        when(useCase.updateGeneralSettings(any(), any(), any(), any(), any(boolean.class), any(), any(), any(), any()))
                .thenReturn(Tenant.create(new TenantId(tenantId), "s", "N"));

        var view = controller.updateGeneralSettings(
                tenantId, new MockHttpSession(), "INTERNAL", false, "a,,b, ,c", "Name", null, null, null, null);

        assertThat(view).isEqualTo("redirect:/tenant/admin/settings?tab=general");
        verify(useCase)
                .updateGeneralSettings(
                        eq(new TenantId(tenantId)),
                        eq("Name"),
                        any(),
                        eq("INTERNAL"),
                        eq(false),
                        eq(Set.of("a", "b", "c")),
                        any(),
                        any(),
                        any());
    }

    @Test
    @DisplayName("SWR-073: updateOidcSettings delegates and redirects")
    void updateOidcSettings() {
        var tenantId = UUID.randomUUID();
        when(useCase.updateOidcSettings(any(), any(), any(), any(), any(), any(boolean.class), any()))
                .thenReturn(Tenant.create(new TenantId(tenantId), "s", "N"));

        var view = controller.updateOidcSettings(
                tenantId, new MockHttpSession(), "https://iss", "cid", "cs", null, false, Map.of());

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
    @DisplayName("switchTenant redirects to default when referer is external URL (open redirect protection)")
    void switchTenantExternalReferer() {
        var session = new MockHttpSession();
        var request = new MockHttpServletRequest();
        request.setServerName("myapp.example.com");
        request.addHeader("Referer", "https://evil.example.com/attack");
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

    @Test
    @DisplayName("switchTenant redirects to default when referer is malformed URL")
    void switchTenantMalformedReferer() {
        var session = new MockHttpSession();
        var request = new MockHttpServletRequest();
        request.addHeader("Referer", "://invalid-url");
        var tid = UUID.randomUUID();

        var view = controller.switchTenant(tid, session, request);

        assertThat(view).isEqualTo("redirect:/tenant/admin/tenants");
    }

    @Test
    @DisplayName("switchTenant allows redirect to absolute URL on same host")
    void switchTenantSameHostAbsoluteUrl() {
        var session = new MockHttpSession();
        var request = new MockHttpServletRequest();
        request.setServerName("myapp.example.com");
        request.addHeader("Referer", "https://myapp.example.com/tenant/admin/settings");
        var tid = UUID.randomUUID();

        var view = controller.switchTenant(tid, session, request);

        assertThat(view).isEqualTo("redirect:https://myapp.example.com/tenant/admin/settings");
    }

    @Test
    @DisplayName("SWR-094: updateGeneralSettings passes defaultRole, logoUrl, faviconUrl")
    void updateGeneralSettingsWithNewFields() {
        var tenantId = UUID.randomUUID();
        when(useCase.updateGeneralSettings(any(), any(), any(), any(), any(boolean.class), any(), any(), any(), any()))
                .thenReturn(Tenant.create(new TenantId(tenantId), "s", "N"));

        var view = controller.updateGeneralSettings(
                tenantId,
                new MockHttpSession(),
                "INTERNAL",
                false,
                "",
                "Name",
                "Tag",
                "AUTHOR",
                "https://logo.png",
                "https://favicon.ico");

        assertThat(view).isEqualTo("redirect:/tenant/admin/settings?tab=general");
        verify(useCase)
                .updateGeneralSettings(
                        eq(new TenantId(tenantId)),
                        eq("Name"),
                        eq("Tag"),
                        eq("INTERNAL"),
                        eq(false),
                        eq(Set.of()),
                        eq("AUTHOR"),
                        eq("https://logo.png"),
                        eq("https://favicon.ico"));
    }

    @Test
    @DisplayName("SWR-092: updateOidcSettings passes oidcButtonText and role mapping enabled")
    void updateOidcSettingsWithButtonTextAndRoleMappingEnabled() {
        var tenantId = UUID.randomUUID();
        when(useCase.updateOidcSettings(any(), any(), any(), any(), any(), any(boolean.class), any()))
                .thenReturn(Tenant.create(new TenantId(tenantId), "s", "N"));

        var view = controller.updateOidcSettings(
                tenantId,
                new MockHttpSession(),
                "https://iss",
                "cid",
                "cs",
                "Login with SSO",
                true,
                Map.of(
                        "oidcIssuerUrl", "https://iss",
                        "oidcRoleMapping_admins", "ADMIN",
                        "oidcRoleMapping_editors", "AUTHOR"));

        assertThat(view).isEqualTo("redirect:/tenant/admin/settings?tab=oidc");
        verify(useCase)
                .updateOidcSettings(
                        eq(new TenantId(tenantId)),
                        eq("https://iss"),
                        eq("cid"),
                        eq("cs"),
                        eq("Login with SSO"),
                        eq(true),
                        eq(Map.of("admins", "ADMIN", "editors", "AUTHOR")));
    }

    @Test
    @DisplayName("SWR-095: extractRoleMappings ignores non-mapping params")
    void updateOidcSettingsIgnoresNonMappingParams() {
        var tenantId = UUID.randomUUID();
        when(useCase.updateOidcSettings(any(), any(), any(), any(), any(), any(boolean.class), any()))
                .thenReturn(Tenant.create(new TenantId(tenantId), "s", "N"));

        controller.updateOidcSettings(
                tenantId,
                new MockHttpSession(),
                null,
                null,
                null,
                null,
                false,
                Map.of("otherParam", "value", "oidcClientId", "cid"));

        verify(useCase).updateOidcSettings(any(), any(), any(), any(), any(), eq(false), eq(Map.of()));
    }

    @Test
    @DisplayName("SWR-095: extractRoleMappings skips blank values")
    void updateOidcSettingsSkipsBlankValues() {
        var tenantId = UUID.randomUUID();
        when(useCase.updateOidcSettings(any(), any(), any(), any(), any(), any(boolean.class), any()))
                .thenReturn(Tenant.create(new TenantId(tenantId), "s", "N"));

        controller.updateOidcSettings(
                tenantId,
                new MockHttpSession(),
                null,
                null,
                null,
                null,
                false,
                Map.of("oidcRoleMapping_admins", " ", "oidcRoleMapping_editors", "AUTHOR"));

        verify(useCase)
                .updateOidcSettings(any(), any(), any(), any(), any(), eq(false), eq(Map.of("editors", "AUTHOR")));
    }

    @Test
    @DisplayName("SWR-095: extractRoleMappings skips blank group name")
    void updateOidcSettingsSkipsBlankGroupName() {
        var tenantId = UUID.randomUUID();
        when(useCase.updateOidcSettings(any(), any(), any(), any(), any(), any(boolean.class), any()))
                .thenReturn(Tenant.create(new TenantId(tenantId), "s", "N"));

        var params = new HashMap<String, String>();
        params.put("oidcRoleMapping_", "ADMIN");
        params.put("oidcRoleMapping_valid", "AUTHOR");
        controller.updateOidcSettings(tenantId, new MockHttpSession(), null, null, null, null, false, params);

        verify(useCase).updateOidcSettings(any(), any(), any(), any(), any(), eq(false), eq(Map.of("valid", "AUTHOR")));
    }
}
