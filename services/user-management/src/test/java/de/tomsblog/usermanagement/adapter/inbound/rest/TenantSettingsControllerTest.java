package de.tomsblog.usermanagement.adapter.inbound.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TenantSettingsController.class)
@Import(TenantSettingsControllerTest.TestSecurityConfig.class)
@TestPropertySource(properties = {"service.api-key=test-api-key"})
class TenantSettingsControllerTest {

    private static final UUID TENANT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final TenantId TENANT_ID = TenantId.of(TENANT_UUID);
    private static final String API_KEY = "test-api-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TenantSettingsUseCase tenantSettingsUseCase;

    @TestConfiguration
    static class TestSecurityConfig {
        @Value("${service.api-key:#{null}}")
        private String apiKey;

        @Bean
        SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
            http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .csrf(csrf -> csrf.disable())
                    .formLogin(form -> form.disable())
                    .httpBasic(basic -> basic.disable())
                    .addFilterBefore(
                            new ApiKeyAuthenticationFilter(apiKey), UsernamePasswordAuthenticationFilter.class);
            return http.build();
        }
    }

    @Test
    @DisplayName("SWR-044: GET /api/tenants/{tenantId}/settings returns settings")
    void getSettings() throws Exception {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
                LoginMode.OIDC,
                true,
                Set.of("example.com"),
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
        when(tenantSettingsUseCase.getSettings(TENANT_ID)).thenReturn(settings);

        mockMvc.perform(get("/api/tenants/{tenantId}/settings", TENANT_UUID).header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(TENANT_UUID.toString()))
                .andExpect(jsonPath("$.loginMode").value("OIDC"))
                .andExpect(jsonPath("$.autoApproveOidc").value(true))
                .andExpect(jsonPath("$.autoApproveEmailDomains[0]").value("example.com"));
    }

    @Test
    @DisplayName("SWR-044: PUT /api/tenants/{tenantId}/settings/login-mode updates login mode")
    void updateLoginMode() throws Exception {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
                LoginMode.INTERNAL,
                false,
                Set.of(),
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
        when(tenantSettingsUseCase.updateLoginMode(eq(TENANT_ID), eq(LoginMode.INTERNAL)))
                .thenReturn(settings);

        var request = new TenantSettingsController.UpdateLoginModeRequest(LoginMode.INTERNAL);

        mockMvc.perform(put("/api/tenants/{tenantId}/settings/login-mode", TENANT_UUID)
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginMode").value("INTERNAL"));
    }

    @Test
    @DisplayName("SWR-044: PUT /api/tenants/{tenantId}/settings/login-mode rejects null loginMode")
    void updateLoginModeRejectsNull() throws Exception {
        mockMvc.perform(put("/api/tenants/{tenantId}/settings/login-mode", TENANT_UUID)
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginMode\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("SWR-045: PUT /api/tenants/{tenantId}/settings/auto-approval updates auto-approval")
    void updateAutoApproval() throws Exception {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
                LoginMode.BOTH,
                true,
                Set.of("test.com"),
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
        when(tenantSettingsUseCase.updateAutoApproval(eq(TENANT_ID), eq(true), any()))
                .thenReturn(settings);

        var request = new TenantSettingsController.UpdateAutoApprovalRequest(true, Set.of("test.com"));

        mockMvc.perform(put("/api/tenants/{tenantId}/settings/auto-approval", TENANT_UUID)
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoApproveOidc").value(true))
                .andExpect(jsonPath("$.autoApproveEmailDomains[0]").value("test.com"));
    }

    @Test
    @DisplayName("SWR-045: PUT /api/tenants/{tenantId}/settings/auto-approval with null domains uses empty set")
    void updateAutoApprovalNullDomains() throws Exception {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
                LoginMode.BOTH,
                false,
                Set.of(),
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
        when(tenantSettingsUseCase.updateAutoApproval(eq(TENANT_ID), eq(false), eq(Set.of())))
                .thenReturn(settings);

        mockMvc.perform(put("/api/tenants/{tenantId}/settings/auto-approval", TENANT_UUID)
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autoApproveOidc\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoApproveOidc").value(false));
    }
}
