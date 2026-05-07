package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
 * Tests verifying the security configuration for the blog content service.
 *
 * @req SWR-016
 * @req SWR-028
 * @req SWR-044
 */
@WebMvcTest({BlogViewController.class, LoginController.class, RegistrationController.class})
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

        @Test
        @DisplayName("GET /login is publicly accessible")
        void login_isPublic() throws Exception {
            mockMvc.perform(get("/login")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("SWR-060: GET /register is publicly accessible")
        void register_isPublic() throws Exception {
            when(userManagementClient.getTenantSettings(any())).thenReturn(null);

            mockMvc.perform(get("/register")
                            .header("X-Tenant-Id", java.util.UUID.randomUUID().toString()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("SWR-028: Protected web endpoints require authentication")
    class ProtectedWebEndpoints {

        @Test
        @DisplayName("GET /posts/new redirects to login when not authenticated")
        void newPost_redirectsToLogin() throws Exception {
            mockMvc.perform(get("/posts/new").accept("text/html"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @DisplayName("GET /posts/{id}/edit redirects to login when not authenticated")
        void editPost_redirectsToLogin() throws Exception {
            mockMvc.perform(get("/posts/" + java.util.UUID.randomUUID() + "/edit")
                            .accept("text/html"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @DisplayName("POST /posts is denied when not authenticated (CSRF blocks)")
        void createPost_redirectsToLogin() throws Exception {
            mockMvc.perform(post("/posts").accept("text/html")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /posts/{id} redirects to login when not authenticated")
        void updatePost_redirectsToLogin() throws Exception {
            mockMvc.perform(post("/posts/" + java.util.UUID.randomUUID())
                            .with(csrf())
                            .accept("text/html"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @DisplayName("POST /posts/{id}/publish redirects to login when not authenticated")
        void publishPost_redirectsToLogin() throws Exception {
            mockMvc.perform(post("/posts/" + java.util.UUID.randomUUID() + "/publish")
                            .with(csrf())
                            .accept("text/html"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
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
        @DisplayName("POST /api/posts returns 401 when not authenticated")
        void createPostApi_returns401() throws Exception {
            mockMvc.perform(post("/api/posts")
                            .contentType("application/json")
                            .content("{\"title\":\"test\",\"content\":\"test\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/posts returns 401 when not authenticated")
        void listPostsApi_returns401() throws Exception {
            mockMvc.perform(get("/api/posts")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("DELETE /api/posts/{id} returns 401 when not authenticated")
        void deletePostApi_returns401() throws Exception {
            mockMvc.perform(delete("/api/posts/" + java.util.UUID.randomUUID())).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("SWR-044: Admin login endpoint")
    class AdminLoginEndpoint {

        @Test
        @DisplayName("GET /admin/login is publicly accessible")
        void adminLogin_isPublic() throws Exception {
            mockMvc.perform(get("/admin/login")).andExpect(status().isOk());
        }
    }
}
