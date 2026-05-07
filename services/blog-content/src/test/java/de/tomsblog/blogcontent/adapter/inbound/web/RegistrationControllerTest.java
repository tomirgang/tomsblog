package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.TenantSettingsDto;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserProfileDto;
import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.application.port.outbound.MarkdownRenderer;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests for the registration controller.
 *
 * @req SWR-059
 * @req SWR-060
 */
@WebMvcTest({RegistrationController.class, LoginController.class})
@Import(SecurityConfiguration.class)
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostUseCase postUseCase;

    @MockitoBean
    private MarkdownRenderer markdownRenderer;

    @MockitoBean
    private UserManagementClient userManagementClient;

    private static final UUID TENANT_ID = UUID.randomUUID();

    private TenantSettingsDto settingsWithMode(String mode) {
        return new TenantSettingsDto(TENANT_ID, mode, false, Set.of(), "Test", null, null, null, null, null, null);
    }

    @Nested
    @DisplayName("GET /register")
    class GetRegister {

        @Test
        @DisplayName("SWR-060: shows registration form when login mode is INTERNAL")
        void showsFormForInternal() throws Exception {
            when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settingsWithMode("INTERNAL"));

            mockMvc.perform(get("/register").header("X-Tenant-Id", TENANT_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"));
        }

        @Test
        @DisplayName("SWR-060: shows registration form when login mode is BOTH")
        void showsFormForBoth() throws Exception {
            when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settingsWithMode("BOTH"));

            mockMvc.perform(get("/register").header("X-Tenant-Id", TENANT_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"));
        }

        @Test
        @DisplayName("SWR-060: redirects to login when login mode is OIDC")
        void redirectsForOidc() throws Exception {
            when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settingsWithMode("OIDC"));

            mockMvc.perform(get("/register").header("X-Tenant-Id", TENANT_ID.toString()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }

        @Test
        @DisplayName("SWR-060: defaults to BOTH when tenant settings are null")
        void defaultsToBothWhenNull() throws Exception {
            when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(null);

            mockMvc.perform(get("/register").header("X-Tenant-Id", TENANT_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"));
        }

        @Test
        @DisplayName("SWR-060: defaults to BOTH when tenant settings lookup fails")
        void defaultsToBothOnError() throws Exception {
            when(userManagementClient.getTenantSettings(TENANT_ID)).thenThrow(new RuntimeException("gRPC error"));

            mockMvc.perform(get("/register").header("X-Tenant-Id", TENANT_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"));
        }
    }

    @Nested
    @DisplayName("POST /register")
    class PostRegister {

        @Test
        @DisplayName("SWR-059: successful registration redirects to success page")
        void successfulRegistration() throws Exception {
            when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settingsWithMode("BOTH"));
            when(userManagementClient.registerUser(
                            eq("newuser"), eq("securePassw0rd"), eq("new@example.com"), eq("New User"), eq(TENANT_ID)))
                    .thenReturn(new UserProfileDto(
                            UUID.randomUUID(),
                            null,
                            "newuser",
                            "INTERNAL",
                            "new@example.com",
                            "New User",
                            "PENDING",
                            List.of("READER"),
                            List.of()));

            mockMvc.perform(post("/register")
                            .header("X-Tenant-Id", TENANT_ID.toString())
                            .param("username", "newuser")
                            .param("email", "new@example.com")
                            .param("displayName", "New User")
                            .param("password", "securePassw0rd")
                            .param("passwordConfirm", "securePassw0rd")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("registration-success"));
        }

        @Test
        @DisplayName("SWR-060: password mismatch shows error")
        void passwordMismatch() throws Exception {
            when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settingsWithMode("BOTH"));

            mockMvc.perform(post("/register")
                            .header("X-Tenant-Id", TENANT_ID.toString())
                            .param("username", "newuser")
                            .param("email", "new@example.com")
                            .param("password", "securePassw0rd")
                            .param("passwordConfirm", "differentPassw0rd")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @DisplayName("SWR-060: short password shows error")
        void shortPassword() throws Exception {
            when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settingsWithMode("BOTH"));

            mockMvc.perform(post("/register")
                            .header("X-Tenant-Id", TENANT_ID.toString())
                            .param("username", "newuser")
                            .param("email", "new@example.com")
                            .param("password", "short")
                            .param("passwordConfirm", "short")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @DisplayName("SWR-059: duplicate user shows conflict error")
        void duplicateUser() throws Exception {
            when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settingsWithMode("BOTH"));
            when(userManagementClient.registerUser(any(), any(), any(), any(), any()))
                    .thenThrow(new StatusRuntimeException(
                            Status.ALREADY_EXISTS.withDescription("Username already taken")));

            mockMvc.perform(post("/register")
                            .header("X-Tenant-Id", TENANT_ID.toString())
                            .param("username", "existing")
                            .param("email", "existing@example.com")
                            .param("password", "securePassw0rd")
                            .param("passwordConfirm", "securePassw0rd")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @DisplayName("SWR-060: redirects to login when login mode is OIDC")
        void redirectsForOidc() throws Exception {
            when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settingsWithMode("OIDC"));

            mockMvc.perform(post("/register")
                            .header("X-Tenant-Id", TENANT_ID.toString())
                            .param("username", "newuser")
                            .param("email", "new@example.com")
                            .param("password", "securePassw0rd")
                            .param("passwordConfirm", "securePassw0rd")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }

        @Test
        @DisplayName("SWR-060: blank username shows error")
        void blankUsername() throws Exception {
            when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settingsWithMode("BOTH"));

            mockMvc.perform(post("/register")
                            .header("X-Tenant-Id", TENANT_ID.toString())
                            .param("username", "")
                            .param("email", "new@example.com")
                            .param("password", "securePassw0rd")
                            .param("passwordConfirm", "securePassw0rd")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @DisplayName("SWR-060: blank email shows error")
        void blankEmail() throws Exception {
            when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settingsWithMode("BOTH"));

            mockMvc.perform(post("/register")
                            .header("X-Tenant-Id", TENANT_ID.toString())
                            .param("username", "newuser")
                            .param("email", "")
                            .param("password", "securePassw0rd")
                            .param("passwordConfirm", "securePassw0rd")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @DisplayName("SWR-059: invalid argument error shows description")
        void invalidArgument() throws Exception {
            when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settingsWithMode("BOTH"));
            when(userManagementClient.registerUser(any(), any(), any(), any(), any()))
                    .thenThrow(new StatusRuntimeException(
                            Status.INVALID_ARGUMENT.withDescription("email must not be blank")));

            mockMvc.perform(post("/register")
                            .header("X-Tenant-Id", TENANT_ID.toString())
                            .param("username", "newuser")
                            .param("email", "new@example.com")
                            .param("password", "securePassw0rd")
                            .param("passwordConfirm", "securePassw0rd")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"))
                    .andExpect(model().attribute("error", "email must not be blank"));
        }

        @Test
        @DisplayName("SWR-059: unexpected gRPC error shows generic message")
        void unexpectedError() throws Exception {
            when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settingsWithMode("BOTH"));
            when(userManagementClient.registerUser(any(), any(), any(), any(), any()))
                    .thenThrow(new StatusRuntimeException(Status.INTERNAL.withDescription("Internal error")));

            mockMvc.perform(post("/register")
                            .header("X-Tenant-Id", TENANT_ID.toString())
                            .param("username", "newuser")
                            .param("email", "new@example.com")
                            .param("password", "securePassw0rd")
                            .param("passwordConfirm", "securePassw0rd")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"))
                    .andExpect(model().attributeExists("error"));
        }
    }
}
