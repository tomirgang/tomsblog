package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BlogViewController.class)
class BlogViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostUseCase postUseCase;

    private final UUID tenantId = UUID.randomUUID();
    private final AuthorId authorId = AuthorId.generate();

    @Test
    @DisplayName("SWR-025: GET / returns index view")
    void index_returnsIndexView() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("index"));
    }

    @Test
    @DisplayName("SWR-026: GET /posts returns post list view with published posts")
    void listPosts_returnsListView() throws Exception {
        Post post = Post.create(TenantId.of(tenantId), authorId, "Published Post", "Content", PostLocale.german());
        post.publish();
        when(postUseCase.listPublishedPosts(any(TenantId.class))).thenReturn(List.of(post));

        mockMvc.perform(get("/posts").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"))
                .andExpect(model().attributeExists("posts"));
    }

    @Test
    @DisplayName("SWR-026: GET /posts returns empty list when no published posts")
    void listPosts_returnsEmptyList() throws Exception {
        when(postUseCase.listPublishedPosts(any(TenantId.class))).thenReturn(List.of());

        mockMvc.perform(get("/posts").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"))
                .andExpect(model().attribute("posts", List.of()));
    }

    @Test
    @DisplayName("SWR-026: GET /posts/{slug} returns post detail view")
    void showPost_returnsShowView() throws Exception {
        Post post = Post.create(TenantId.of(tenantId), authorId, "My Post", "Full content here", PostLocale.german());
        post.publish();
        when(postUseCase.getPublishedPostBySlug(any(Slug.class), any(TenantId.class)))
                .thenReturn(post);

        mockMvc.perform(get("/posts/my-post").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/show"))
                .andExpect(model().attributeExists("post"));
    }

    @Test
    @DisplayName("SWR-026: GET /posts/{slug} returns 404 when post not found")
    void showPost_returns404WhenNotFound() throws Exception {
        when(postUseCase.getPublishedPostBySlug(any(Slug.class), any(TenantId.class)))
                .thenThrow(new PostNotFoundException(new Slug("nonexistent")));

        mockMvc.perform(get("/posts/nonexistent").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNotFound());
    }
}
