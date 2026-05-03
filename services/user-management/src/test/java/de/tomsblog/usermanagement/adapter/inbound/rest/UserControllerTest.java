package de.tomsblog.usermanagement.adapter.inbound.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tomsblog.usermanagement.application.port.inbound.SyncInternalUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.SyncOidcUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.UserProfileUseCase;
import de.tomsblog.usermanagement.application.service.UserProfileNotFoundException;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserProfileUseCase userProfileUseCase;

    @Test
    @DisplayName("SWR-043: POST /api/users/sync/oidc creates/syncs OIDC user")
    void syncFromOidc() throws Exception {
        var profile = UserProfile.createFromOidc("sub-1", "user@example.com", "User");
        when(userProfileUseCase.syncFromOidc(any(SyncOidcUserCommand.class))).thenReturn(profile);

        var request = new SyncOidcUserRequest("sub-1", "user@example.com", "User", List.of(), TENANT_ID);

        mockMvc.perform(post("/api/users/sync/oidc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oidcSubject").value("sub-1"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.authSource").value("OIDC"))
                .andExpect(jsonPath("$.approvalStatus").value("PENDING"));
    }

    @Test
    @DisplayName("SWR-043: POST /api/users/sync/oidc rejects blank oidcSubject")
    void syncFromOidcRejectsBlankSubject() throws Exception {
        var request = new SyncOidcUserRequest("", "user@example.com", "User", List.of(), TENANT_ID);

        mockMvc.perform(post("/api/users/sync/oidc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("SWR-044: POST /api/users/sync/internal creates/syncs internal user")
    void syncFromInternal() throws Exception {
        var profile = UserProfile.createInternal("admin", "hash", "admin@example.com", "Admin");
        when(userProfileUseCase.syncFromInternal(any(SyncInternalUserCommand.class)))
                .thenReturn(profile);

        var request = new SyncInternalUserRequest("admin", "hash", "admin@example.com", "Admin", TENANT_ID);

        mockMvc.perform(post("/api/users/sync/internal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.authSource").value("INTERNAL"));
    }

    @Test
    @DisplayName("SWR-043: GET /api/users/by-oidc-subject/{subject} returns profile")
    void findByOidcSubject() throws Exception {
        var profile = UserProfile.createFromOidc("sub-1", "user@example.com", "User");
        when(userProfileUseCase.findByOidcSubject("sub-1")).thenReturn(profile);

        mockMvc.perform(get("/api/users/by-oidc-subject/sub-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oidcSubject").value("sub-1"));
    }

    @Test
    @DisplayName("SWR-043: GET /api/users/by-oidc-subject/{subject} returns 404 when not found")
    void findByOidcSubjectNotFound() throws Exception {
        when(userProfileUseCase.findByOidcSubject("unknown")).thenThrow(new UserProfileNotFoundException("unknown"));

        mockMvc.perform(get("/api/users/by-oidc-subject/unknown")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SWR-043: GET /api/users/by-username/{username} returns profile")
    void findByUsername() throws Exception {
        var profile = UserProfile.createInternal("admin", "hash", "admin@example.com", "Admin");
        when(userProfileUseCase.findByUsername("admin")).thenReturn(profile);

        mockMvc.perform(get("/api/users/by-username/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    @DisplayName("SWR-043: POST /api/users/{id}/approve returns 204")
    void approveUser() throws Exception {
        mockMvc.perform(post("/api/users/sub-1/approve")).andExpect(status().isNoContent());

        verify(userProfileUseCase).approveUser("sub-1");
    }

    @Test
    @DisplayName("SWR-043: POST /api/users/{id}/reject returns 204")
    void rejectUser() throws Exception {
        mockMvc.perform(post("/api/users/sub-1/reject")).andExpect(status().isNoContent());

        verify(userProfileUseCase).rejectUser("sub-1");
    }

    @Test
    @DisplayName("SWR-043: POST /api/users/sync/oidc with null oidcGroups defaults to empty list")
    void syncFromOidcWithNullGroups() throws Exception {
        var profile = UserProfile.createFromOidc("sub-1", "user@example.com", "User");
        when(userProfileUseCase.syncFromOidc(any(SyncOidcUserCommand.class))).thenReturn(profile);

        mockMvc.perform(
                        post("/api/users/sync/oidc")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"oidcSubject\":\"sub-1\",\"email\":\"user@example.com\",\"displayName\":\"User\",\"tenantId\":\"00000000-0000-0000-0000-000000000001\"}"))
                .andExpect(status().isOk());
    }
}
