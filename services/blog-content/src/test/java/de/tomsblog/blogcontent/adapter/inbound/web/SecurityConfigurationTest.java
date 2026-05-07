package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.application.port.outbound.MarkdownRenderer;
import de.tomsblog.blogcontent.domain.model.Post;
import de.tomsblog.blogcontent.domain.model.PostLocale;
import de.tomsblog.blogcontent.domain.model.Slug;
import de.tomsblog.shared.domain.AuthorId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests verifying the security configuration for the blog content service (ADR-0032).
 *
 * <p>Authentication is now handled by the User Management Service. This service reads
 * shared Redis sessions. Unauthenticated users are redirected to /auth/login.
 *
 * @req SWR-016
 * @req SWR-028
 * @req SWR-064
 */
@WebMvcTest(BlogViewController.class)
@Import(SecurityConfiguration.class)
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostUseCase postUseCase;

    @MockitoBean
    private MarkdownRenderer markdownRenderer;

    @MockitoBean
    private UserManagementClient userManagementClient;

    @Nested
    @DisplayName("SWR-028: Public endpoints accessible without authentication")
    class PublicEndpoints {

        @Test
        @DisplayName("GET / is publicly accessible")
        void index_isPublic() throws Exception {
            when(postUseCase.listRecentPublishedPosts(any(TenantId.class), anyInt()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /posts is publicly accessible")
        void listPosts_isPublic() throws Exception {
            when(postUseCase.listPublishedPosts(any(TenantId.class))).thenReturn(List.of());

            mockMvc.perform(get("/posts")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /posts/{slug} is publicly accessible for published posts")
        void showPost_isPublic() throws Exception {
            Post post = Post.create(
                    TenantId.of(UUID.randomUUID()), AuthorId.generate(), "Test Post", "Content", PostLocale.german());
            post.publish();
            when(postUseCase.getPublishedPostBySlug(any(Slug.class), any(TenantId.class)))
                    .thenReturn(post);

            mockMvc.perform(get("/posts/some-slug")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("SWR-028: Protected web endpoints redirect to auth service")
    class ProtectedWebEndpoints {

        @Test
        @DisplayName("GET /posts/new redirects to /auth/login when not authenticated")
        void newPost_redirectsToAuthLogin() throws Exception {
            mockMvc.perform(get("/posts/new").accept("text/html"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/auth/login"));
        }

        @Test
        @DisplayName("GET /posts/{id}/edit redirects to /auth/login when not authenticated")
        void editPost_redirectsToAuthLogin() throws Exception {
            mockMvc.perform(get("/posts/" + UUID.randomUUID() + "/edit").accept("text/html"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/auth/login"));
        }
    }

    @Nested
    @DisplayName("SWR-028: Protected web endpoints accessible when authenticated")
    @WithMockUser(username = "admin", roles = "ADMIN")
    class AuthenticatedWebEndpoints {

        @Test
        @DisplayName("GET /posts/new is accessible when authenticated")
        void newPost_isAccessible() throws Exception {
            mockMvc.perform(get("/posts/new")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("SWR-028: REST API requires authentication")
    class ProtectedApiEndpoints {

        @Test
        @DisplayName("GET /api/posts returns 401 when not authenticated")
        void listPostsApi_returns401() throws Exception {
            mockMvc.perform(get("/api/posts")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("DELETE /api/posts/{id} returns 401 when not authenticated")
        void deletePostApi_returns401() throws Exception {
            mockMvc.perform(delete("/api/posts/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        }
    }
}
