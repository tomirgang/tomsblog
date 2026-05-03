package de.tomsblog.blogcontent.adapter.inbound.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import de.tomsblog.blogcontent.adapter.inbound.web.SecurityConfiguration;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import de.tomsblog.blogcontent.application.port.inbound.TagUseCase;
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
 * Fuzz tests for TagController REST endpoints.
 *
 * @req SWR-023
 */
@WebMvcTest(TagController.class)
@Import(SecurityConfiguration.class)
@WithMockUser(username = "admin", roles = "ADMIN")
@Tag("fuzz")
@SuppressWarnings("null")
class TagControllerFuzzTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagUseCase tagUseCase;

    @MockitoBean
    private UserManagementClient userManagementClient;

    @FuzzTest(maxDuration = "30s")
    @DisplayName("SWR-023: POST /api/tags must not return 5xx for any input")
    void fuzzCreateTag(FuzzedDataProvider data) throws Exception {
        String json = data.consumeRemainingAsString();

        mockMvc.perform(post("/api/tags")
                        .header("X-Tenant-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is(org.hamcrest.Matchers.lessThan(500)));
    }

    @FuzzTest(maxDuration = "30s")
    @DisplayName("SWR-023: PUT /api/tags/{id} must not return 5xx for any input")
    void fuzzRenameTag(FuzzedDataProvider data) throws Exception {
        String json = data.consumeRemainingAsString();

        mockMvc.perform(put("/api/tags/{id}", UUID.randomUUID())
                        .header("X-Tenant-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is(org.hamcrest.Matchers.lessThan(500)));
    }
}
