package de.tomsblog.blogcontent.adapter.inbound.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import de.tomsblog.blogcontent.adapter.inbound.web.SecurityConfiguration;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import de.tomsblog.blogcontent.application.port.inbound.TranslationUseCase;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Fuzz tests for TranslationController REST endpoints.
 *
 * @req SWR-023
 */
@WebMvcTest(TranslationController.class)
@Import(SecurityConfiguration.class)
@WithMockUser(username = "admin", roles = "ADMIN")
@Tag("fuzz")
@SuppressWarnings("null")
class TranslationControllerFuzzTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TranslationUseCase translationUseCase;

    @MockitoBean
    private UserManagementClient userManagementClient;

    @FuzzTest(maxDuration = "30s")
    @DisplayName("SWR-023: POST /api/posts/{postId}/translations must not return 5xx for any input")
    void fuzzCreateTranslation(FuzzedDataProvider data) throws Exception {
        String json = data.consumeRemainingAsString();
        UUID postId = UUID.randomUUID();

        mockMvc.perform(post("/api/posts/{postId}/translations", postId)
                        .with(csrf())
                        .header("X-Tenant-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is(org.hamcrest.Matchers.lessThan(500)));
    }

    @FuzzTest(maxDuration = "30s")
    @DisplayName("SWR-023: PUT /api/posts/{postId}/translations/{id} must not return 5xx for any input")
    void fuzzUpdateTranslation(FuzzedDataProvider data) throws Exception {
        String json = data.consumeRemainingAsString();
        UUID postId = UUID.randomUUID();
        UUID translationId = UUID.randomUUID();

        mockMvc.perform(put("/api/posts/{postId}/translations/{id}", postId, translationId)
                        .with(csrf())
                        .header("X-Tenant-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is(org.hamcrest.Matchers.lessThan(500)));
    }
}
