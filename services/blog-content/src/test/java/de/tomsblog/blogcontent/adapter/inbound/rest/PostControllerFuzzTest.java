package de.tomsblog.blogcontent.adapter.inbound.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import de.tomsblog.blogcontent.adapter.inbound.web.SecurityConfiguration;
import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
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
 * Fuzz tests for PostController REST endpoints.
 *
 * @req SWR-023
 */
@WebMvcTest(PostController.class)
@Import(SecurityConfiguration.class)
@WithMockUser(username = "admin", roles = "ADMIN")
@Tag("fuzz")
@SuppressWarnings("null")
class PostControllerFuzzTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostUseCase postUseCase;

    @FuzzTest(maxDuration = "30s")
    @DisplayName("SWR-023: POST /api/posts must not return 5xx for any input")
    void fuzzCreatePost(FuzzedDataProvider data) throws Exception {
        String json = data.consumeRemainingAsString();

        mockMvc.perform(post("/api/posts")
                        .header("X-Tenant-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is(org.hamcrest.Matchers.lessThan(500)));
    }

    @FuzzTest(maxDuration = "30s")
    @DisplayName("SWR-023: PUT /api/posts/{id} must not return 5xx for any input")
    void fuzzUpdatePost(FuzzedDataProvider data) throws Exception {
        String json = data.consumeRemainingAsString();

        mockMvc.perform(put("/api/posts/{id}", UUID.randomUUID())
                        .header("X-Tenant-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is(org.hamcrest.Matchers.lessThan(500)));
    }
}
