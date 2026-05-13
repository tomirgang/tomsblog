package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests the SecurityConfiguration when HTTP Basic authentication is enabled.
 *
 * @req SWR-016
 */
@WebMvcTest(BlogViewController.class)
@Import(SecurityConfiguration.class)
@TestPropertySource(properties = "blog.security.http-basic-enabled=true")
class SecurityConfigurationHttpBasicTest {

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
    @DisplayName("SWR-016: Public endpoints remain accessible when HTTP Basic is enabled")
    void httpBasicEnabled_publicEndpointsStillAccessible() throws Exception {
        when(postUseCase.listRecentPublishedPosts(any(TenantId.class), anyInt()))
                .thenReturn(List.of());

        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("SWR-016: Authenticated endpoints work with HTTP Basic enabled")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void httpBasicEnabled_authenticatedEndpointsWork() throws Exception {
        Post post = Post.create(
                TenantId.of(UUID.randomUUID()), AuthorId.generate(), "Test", "Content", PostLocale.german());
        when(postUseCase.listPosts(any(TenantId.class))).thenReturn(List.of(post));

        mockMvc.perform(get("/posts/new")).andExpect(status().isOk());
    }
}
