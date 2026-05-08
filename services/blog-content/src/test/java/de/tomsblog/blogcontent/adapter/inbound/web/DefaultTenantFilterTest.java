package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.application.port.inbound.TagUseCase;
import de.tomsblog.blogcontent.application.port.outbound.MarkdownRenderer;
import de.tomsblog.blogcontent.domain.model.Post;
import de.tomsblog.blogcontent.domain.model.PostLocale;
import de.tomsblog.shared.domain.AuthorId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BlogViewController.class)
@Import({DefaultTenantFilter.class, SecurityConfiguration.class})
@WithMockUser(username = "admin", roles = "ADMIN")
class DefaultTenantFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostUseCase postUseCase;

    @MockitoBean
    private TagUseCase tagUseCase;

    @MockitoBean
    private MarkdownRenderer markdownRenderer;

    @MockitoBean
    private UserManagementClient userManagementClient;

    @Test
    @DisplayName("SWR-027: Filter injects default headers when both are missing")
    void missingBothHeaders_usesDefaults() throws Exception {
        Post post = Post.create(
                TenantId.of(UUID.randomUUID()), AuthorId.generate(), "Test", "Content", PostLocale.german());
        Mockito.when(postUseCase.createPost(Mockito.any())).thenReturn(post);

        mockMvc.perform(post("/posts")
                        .with(csrf())
                        .param("title", "Test")
                        .param("content", "Content")
                        .param("contentType", "HTML")
                        .param("locale", "de"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("SWR-027: Filter injects default X-Author-Id when only tenant is present")
    void missingAuthorHeader_onlyAuthorInjected() throws Exception {
        Post post = Post.create(
                TenantId.of(UUID.randomUUID()), AuthorId.generate(), "Test", "Content", PostLocale.german());
        Mockito.when(postUseCase.createPost(Mockito.any())).thenReturn(post);

        mockMvc.perform(post("/posts")
                        .with(csrf())
                        .header("X-Tenant-Id", "11111111-1111-1111-1111-111111111111")
                        .param("title", "Test")
                        .param("content", "Content")
                        .param("contentType", "HTML")
                        .param("locale", "de"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("SWR-027: Filter injects default X-Tenant-Id when only author is present")
    void missingTenantHeader_onlyTenantInjected() throws Exception {
        Mockito.when(postUseCase.listPosts(Mockito.any())).thenReturn(List.of());

        mockMvc.perform(get("/posts").header("X-Author-Id", "22222222-2222-2222-2222-222222222222"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"));
    }

    @Test
    @DisplayName("SWR-027: Filter does not wrap request when both headers are present")
    void bothHeadersPresent_noWrapping() throws Exception {
        Mockito.when(postUseCase.listPosts(Mockito.any())).thenReturn(List.of());

        mockMvc.perform(get("/posts")
                        .header("X-Tenant-Id", "11111111-1111-1111-1111-111111111111")
                        .header("X-Author-Id", "22222222-2222-2222-2222-222222222222"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"));
    }

    @Test
    @DisplayName("SWR-053: Filter overrides tenant header with session value")
    void sessionTenantOverride() throws Exception {
        Mockito.when(postUseCase.listPosts(Mockito.any())).thenReturn(List.of());
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("activeTenantId", "33333333-3333-3333-3333-333333333333");

        mockMvc.perform(get("/posts")
                        .session(session)
                        .header("X-Tenant-Id", "11111111-1111-1111-1111-111111111111")
                        .header("X-Author-Id", "22222222-2222-2222-2222-222222222222"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"));
    }
}
