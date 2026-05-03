package de.tomsblog.blogcontent.adapter.inbound.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.tomsblog.blogcontent.adapter.inbound.web.SecurityConfiguration;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.application.service.PostNotFoundException;
import de.tomsblog.blogcontent.domain.model.*;
import de.tomsblog.shared.domain.AuthorId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SourceController.class)
@Import(SecurityConfiguration.class)
@WithMockUser(username = "admin", roles = "ADMIN")
@SuppressWarnings("null")
class SourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostUseCase postUseCase;

    @MockitoBean
    private UserManagementClient userManagementClient;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID postId = UUID.randomUUID();

    @Test
    @DisplayName("SWR-012: POST /api/posts/{postId}/sources adds source and returns 200")
    void addSource_returns200() throws Exception {
        Post post =
                Post.create(TenantId.of(tenantId), AuthorId.generate(), "Test Post", "Content", PostLocale.german());
        when(postUseCase.addSource(any())).thenReturn(post);

        String body = """
                {
                    "url": "https://example.com/article",
                    "title": "Example Article"
                }
                """;

        mockMvc.perform(post("/api/posts/{postId}/sources", postId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://example.com/article"))
                .andExpect(jsonPath("$.title").value("Example Article"));
    }

    @Test
    @DisplayName("SWR-012: POST /api/posts/{postId}/sources returns 400 for blank URL")
    void addSource_returns400ForBlankUrl() throws Exception {
        String body = """
                {
                    "url": "",
                    "title": "Example Article"
                }
                """;

        mockMvc.perform(post("/api/posts/{postId}/sources", postId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("SWR-012: POST /api/posts/{postId}/sources returns 400 for blank title")
    void addSource_returns400ForBlankTitle() throws Exception {
        String body = """
                {
                    "url": "https://example.com",
                    "title": ""
                }
                """;

        mockMvc.perform(post("/api/posts/{postId}/sources", postId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("SWR-012: POST /api/posts/{postId}/sources returns 404 when post not found")
    void addSource_returns404WhenPostNotFound() throws Exception {
        when(postUseCase.addSource(any())).thenThrow(new PostNotFoundException(PostId.of(postId)));

        String body = """
                {
                    "url": "https://example.com",
                    "title": "Example"
                }
                """;

        mockMvc.perform(post("/api/posts/{postId}/sources", postId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SWR-012: GET /api/posts/{postId}/sources returns list of sources")
    void listSources_returns200() throws Exception {
        List<Source> sources =
                List.of(new Source("https://example.com", "Example"), new Source("https://docs.spring.io", "Spring"));
        when(postUseCase.listSources(any(), any())).thenReturn(sources);

        mockMvc.perform(get("/api/posts/{postId}/sources", postId).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].url").value("https://example.com"))
                .andExpect(jsonPath("$[0].title").value("Example"))
                .andExpect(jsonPath("$[1].url").value("https://docs.spring.io"))
                .andExpect(jsonPath("$[1].title").value("Spring"));
    }

    @Test
    @DisplayName("SWR-012: GET /api/posts/{postId}/sources returns empty list")
    void listSources_returnsEmptyList() throws Exception {
        when(postUseCase.listSources(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/posts/{postId}/sources", postId).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("SWR-012: GET /api/posts/{postId}/sources returns 404 when post not found")
    void listSources_returns404WhenPostNotFound() throws Exception {
        when(postUseCase.listSources(any(), any())).thenThrow(new PostNotFoundException(PostId.of(postId)));

        mockMvc.perform(get("/api/posts/{postId}/sources", postId).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SWR-012: DELETE /api/posts/{postId}/sources removes source and returns 204")
    void removeSource_returns204() throws Exception {
        Post post =
                Post.create(TenantId.of(tenantId), AuthorId.generate(), "Test Post", "Content", PostLocale.german());
        when(postUseCase.removeSource(any())).thenReturn(post);

        String body = """
                {
                    "url": "https://example.com/article",
                    "title": "Example Article"
                }
                """;

        mockMvc.perform(delete("/api/posts/{postId}/sources", postId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("SWR-012: DELETE /api/posts/{postId}/sources returns 400 for blank URL")
    void removeSource_returns400ForBlankUrl() throws Exception {
        String body = """
                {
                    "url": "",
                    "title": "Example Article"
                }
                """;

        mockMvc.perform(delete("/api/posts/{postId}/sources", postId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("SWR-012: DELETE /api/posts/{postId}/sources returns 404 when post not found")
    void removeSource_returns404WhenPostNotFound() throws Exception {
        when(postUseCase.removeSource(any())).thenThrow(new PostNotFoundException(PostId.of(postId)));

        String body = """
                {
                    "url": "https://example.com",
                    "title": "Example"
                }
                """;

        mockMvc.perform(delete("/api/posts/{postId}/sources", postId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}
