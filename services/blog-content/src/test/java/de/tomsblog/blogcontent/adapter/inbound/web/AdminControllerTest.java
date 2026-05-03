package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.TenantSettingsDto;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserProfileDto;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminController.class)
@Import({DefaultTenantFilter.class, SecurityConfiguration.class})
class AdminControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserManagementClient userManagementClient;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("SWR-051: GET /admin/users lists users for tenant")
    void listUsers() throws Exception {
        var user = new UserProfileDto(
                UUID.randomUUID(),
                "sub-1",
                "admin",
                "OIDC",
                "admin@test.com",
                "Admin",
                "APPROVED",
                List.of("ADMIN"),
                List.of());
        when(userManagementClient.listUsersByTenant(TENANT_ID)).thenReturn(List.of(user));

        mockMvc.perform(get("/admin/users").header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("SWR-051: POST /admin/users/{id}/approve approves user")
    void approveUser() throws Exception {
        mockMvc.perform(post("/admin/users/sub-1/approve").with(csrf()).header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userManagementClient).approveUser("sub-1");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("SWR-051: POST /admin/users/{id}/reject rejects user")
    void rejectUser() throws Exception {
        mockMvc.perform(post("/admin/users/sub-1/reject").with(csrf()).header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userManagementClient).rejectUser("sub-1");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("SWR-051: POST /admin/users/{id}/role changes role")
    void changeRole() throws Exception {
        mockMvc.perform(post("/admin/users/sub-1/role")
                        .with(csrf())
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .param("role", "AUTHOR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userManagementClient).changeUserRole("sub-1", TENANT_ID, "AUTHOR");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("SWR-052: GET /admin/settings shows tenant settings")
    void showSettings() throws Exception {
        var settings = new TenantSettingsDto(TENANT_ID, "BOTH", false, Set.of(), "My Blog", "A tagline", null, null);
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settings);

        mockMvc.perform(get("/admin/settings").header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settings"))
                .andExpect(model().attributeExists("settings"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("SWR-052: GET /admin/settings uses defaults when client fails")
    void showSettingsFallback() throws Exception {
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenThrow(new RuntimeException("gRPC down"));

        mockMvc.perform(get("/admin/settings").header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settings"))
                .andExpect(model().attributeExists("settings"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("SWR-052: POST /admin/settings updates tenant settings")
    void updateSettings() throws Exception {
        mockMvc.perform(post("/admin/settings")
                        .with(csrf())
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .param("loginMode", "OIDC")
                        .param("autoApproveOidc", "true")
                        .param("autoApproveEmailDomains", "test.com, example.org")
                        .param("displayName", "My Blog")
                        .param("tagline", "A cool blog"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings"));

        verify(userManagementClient)
                .updateTenantSettings(
                        TENANT_ID,
                        "OIDC",
                        true,
                        Set.of("test.com", "example.org"),
                        "My Blog",
                        "A cool blog",
                        null,
                        null);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("SWR-052: POST /admin/settings with empty domains")
    void updateSettingsEmptyDomains() throws Exception {
        mockMvc.perform(post("/admin/settings")
                        .with(csrf())
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .param("loginMode", "BOTH")
                        .param("autoApproveEmailDomains", "")
                        .param("displayName", "Blog")
                        .param("tagline", ""))
                .andExpect(status().is3xxRedirection());

        verify(userManagementClient).updateTenantSettings(TENANT_ID, "BOTH", false, Set.of(), "Blog", "", null, null);
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("SWR-053: POST /admin/switch-tenant stores tenant in session")
    void switchTenant() throws Exception {
        UUID newTenant = UUID.fromString("22222222-2222-2222-2222-222222222222");

        mockMvc.perform(post("/admin/switch-tenant")
                        .with(csrf())
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .param("tenantId", newTenant.toString()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("SWR-053: POST /admin/switch-tenant redirects to referer")
    void switchTenantRedirectsToReferer() throws Exception {
        UUID newTenant = UUID.fromString("22222222-2222-2222-2222-222222222222");

        mockMvc.perform(post("/admin/switch-tenant")
                        .with(csrf())
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .header("Referer", "/admin/settings")
                        .param("tenantId", newTenant.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("SWR-053: GET /admin/users uses session tenant override")
    void listUsersWithSessionOverride() throws Exception {
        UUID sessionTenant = UUID.fromString("33333333-3333-3333-3333-333333333333");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("activeTenantId", sessionTenant.toString());

        when(userManagementClient.listUsersByTenant(sessionTenant)).thenReturn(List.of());

        mockMvc.perform(get("/admin/users").session(session).header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk());

        verify(userManagementClient).listUsersByTenant(sessionTenant);
    }

    @Test
    @WithMockUser(roles = "READER")
    @DisplayName("SWR-051: GET /admin/users denied for READER role")
    void listUsersDeniedForReader() throws Exception {
        mockMvc.perform(get("/admin/users").header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isForbidden());
    }
}
