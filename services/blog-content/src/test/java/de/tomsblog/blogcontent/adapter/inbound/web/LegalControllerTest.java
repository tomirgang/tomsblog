package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.TenantSettingsDto;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LegalController.class)
@Import({DefaultTenantFilter.class, SecurityConfiguration.class})
class LegalControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserManagementClient userManagementClient;

    @Test
    @DisplayName("SWR-054: GET /impressum shows default when no custom content")
    void impressumDefaultContent() throws Exception {
        var settings = new TenantSettingsDto(
                TENANT_ID, "BOTH", false, Set.of(), "Blog", null, null, null, null, null, null, null, null, null, null,
                false, Map.of());
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settings);

        mockMvc.perform(get("/impressum").header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("legal/impressum"))
                .andExpect(model().attributeDoesNotExist("customContent"));
    }

    @Test
    @DisplayName("SWR-055: GET /impressum shows tenant-specific content")
    void impressumCustomContent() throws Exception {
        var settings = new TenantSettingsDto(
                TENANT_ID,
                "BOTH",
                false,
                Set.of(),
                "Blog",
                null,
                "<p>Mein Impressum</p>",
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
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settings);

        mockMvc.perform(get("/impressum").header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("legal/impressum"))
                .andExpect(model().attribute("customContent", "<p>Mein Impressum</p>"));
    }

    @Test
    @DisplayName("SWR-054: GET /privacy shows default when no custom content")
    void privacyDefaultContent() throws Exception {
        var settings = new TenantSettingsDto(
                TENANT_ID, "BOTH", false, Set.of(), "Blog", null, null, null, null, null, null, null, null, null, null,
                false, Map.of());
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settings);

        mockMvc.perform(get("/privacy").header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("legal/privacy"))
                .andExpect(model().attributeDoesNotExist("customContent"));
    }

    @Test
    @DisplayName("SWR-055: GET /privacy shows tenant-specific content")
    void privacyCustomContent() throws Exception {
        var settings = new TenantSettingsDto(
                TENANT_ID,
                "BOTH",
                false,
                Set.of(),
                "Blog",
                null,
                null,
                "<p>Datenschutz</p>",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                Map.of());
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settings);

        mockMvc.perform(get("/privacy").header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("legal/privacy"))
                .andExpect(model().attribute("customContent", "<p>Datenschutz</p>"));
    }

    @Test
    @DisplayName("SWR-054: GET /impressum handles service error gracefully")
    void impressumServiceError() throws Exception {
        when(userManagementClient.getTenantSettings(any())).thenThrow(new RuntimeException("down"));

        mockMvc.perform(get("/impressum").header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("legal/impressum"))
                .andExpect(model().attributeDoesNotExist("customContent"));
    }

    @Test
    @DisplayName("SWR-054: GET /privacy handles service error gracefully")
    void privacyServiceError() throws Exception {
        when(userManagementClient.getTenantSettings(any())).thenThrow(new RuntimeException("down"));

        mockMvc.perform(get("/privacy").header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("legal/privacy"))
                .andExpect(model().attributeDoesNotExist("customContent"));
    }

    @Test
    @DisplayName("SWR-054: GET /impressum uses default tenant when no header")
    void impressumDefaultTenant() throws Exception {
        var settings = new TenantSettingsDto(
                TENANT_ID, "BOTH", false, Set.of(), "Blog", null, null, null, null, null, null, null, null, null, null,
                false, Map.of());
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settings);

        mockMvc.perform(get("/impressum")).andExpect(status().isOk()).andExpect(view().name("legal/impressum"));
    }

    @Test
    @DisplayName("SWR-054: GET /impressum uses session tenant over header")
    void impressumSessionTenant() throws Exception {
        UUID sessionTenant = UUID.fromString("22222222-2222-2222-2222-222222222222");
        var settings = new TenantSettingsDto(
                sessionTenant,
                "BOTH",
                false,
                Set.of(),
                "Blog",
                null,
                "<p>Session Impressum</p>",
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

        when(userManagementClient.getTenantSettings(sessionTenant)).thenReturn(settings);

        mockMvc.perform(get("/impressum")
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .sessionAttr("activeTenantId", sessionTenant.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("customContent", "<p>Session Impressum</p>"));
    }

    @Test
    @DisplayName("SWR-055: GET /impressum treats blank content as no custom content")
    void impressumBlankContent() throws Exception {
        var settings = new TenantSettingsDto(
                TENANT_ID, "BOTH", false, Set.of(), "Blog", null, "   ", null, null, null, null, null, null, null, null,
                false, Map.of());
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settings);

        mockMvc.perform(get("/impressum").header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("customContent"));
    }

    @Test
    @DisplayName("SWR-054: GET /impressum handles invalid session tenant ID")
    void impressumInvalidSessionTenant() throws Exception {
        var settings = new TenantSettingsDto(
                TENANT_ID, "BOTH", false, Set.of(), "Blog", null, null, null, null, null, null, null, null, null, null,
                false, Map.of());
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settings);

        mockMvc.perform(get("/impressum")
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .sessionAttr("activeTenantId", "not-a-uuid"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SWR-054: GET /impressum handles invalid header tenant ID")
    void impressumInvalidHeaderTenant() throws Exception {
        mockMvc.perform(get("/impressum").header("X-Tenant-Id", "not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("SWR-054: GET /privacy handles null settings from client")
    void privacyNullSettings() throws Exception {
        when(userManagementClient.getTenantSettings(any())).thenReturn(null);

        mockMvc.perform(get("/privacy").header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("customContent"));
    }

    @Test
    @DisplayName("SWR-055: GET /privacy treats blank content as no custom content")
    void privacyBlankContent() throws Exception {
        var settings = new TenantSettingsDto(
                TENANT_ID, "BOTH", false, Set.of(), "Blog", null, null, "   ", null, null, null, null, null, null, null,
                false, Map.of());
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settings);

        mockMvc.perform(get("/privacy").header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("customContent"));
    }
}
