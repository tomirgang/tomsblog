package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.tomsblog.blogcontent.application.port.inbound.CreatePostCommand;
import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.application.port.inbound.UpdatePostCommand;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BlogViewController.class)
@Import(SecurityConfiguration.class)
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
    @DisplayName("SWR-028: GET /posts without auth returns only published posts")
    void listPosts_anonymous_returnsPublishedOnly() throws Exception {
        Post post = Post.create(TenantId.of(tenantId), authorId, "Published Post", "Content", PostLocale.german());
        post.publish();
        when(postUseCase.listPublishedPosts(any(TenantId.class))).thenReturn(List.of(post));

        mockMvc.perform(get("/posts").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"))
                .andExpect(model().attributeExists("posts"));

        verify(postUseCase).listPublishedPosts(any(TenantId.class));
        verify(postUseCase, never()).listPosts(any(TenantId.class));
    }

    @Test
    @DisplayName("SWR-028: GET /posts with auth returns all posts")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listPosts_authenticated_returnsAllPosts() throws Exception {
        Post post = Post.create(TenantId.of(tenantId), authorId, "Draft Post", "Content", PostLocale.german());
        when(postUseCase.listPosts(any(TenantId.class))).thenReturn(List.of(post));

        mockMvc.perform(get("/posts").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"))
                .andExpect(model().attributeExists("posts"));

        verify(postUseCase).listPosts(any(TenantId.class));
        verify(postUseCase, never()).listPublishedPosts(any(TenantId.class));
    }

    @Test
    @DisplayName("SWR-026: GET /posts returns empty list when no posts")
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

    @Test
    @DisplayName("SWR-027: GET /posts/new returns empty form")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void newPostForm_returnsEmptyForm() throws Exception {
        mockMvc.perform(get("/posts/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeExists("postForm"))
                .andExpect(model().attribute("editMode", false));
    }

    @Test
    @DisplayName("SWR-027: POST /posts creates post and redirects on valid input")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createPost_validInput_redirects() throws Exception {
        Post post = Post.create(TenantId.of(tenantId), authorId, "New Post", "Content", PostLocale.german());
        when(postUseCase.createPost(any(CreatePostCommand.class))).thenReturn(post);

        mockMvc.perform(post("/posts")
                        .with(csrf())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-Author-Id", authorId.value().toString())
                        .param("title", "New Post")
                        .param("content", "Some content")
                        .param("locale", "de"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        verify(postUseCase).createPost(any(CreatePostCommand.class));
    }

    @Test
    @DisplayName("SWR-027: POST /posts returns form with errors on blank title")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createPost_blankTitle_returnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/posts")
                        .with(csrf())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-Author-Id", authorId.value().toString())
                        .param("title", "")
                        .param("content", "Some content")
                        .param("locale", "de"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeHasFieldErrors("postForm", "title"))
                .andExpect(model().attribute("editMode", false));

        verifyNoInteractions(postUseCase);
    }

    @Test
    @DisplayName("SWR-027: POST /posts returns form with errors on blank content")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createPost_blankContent_returnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/posts")
                        .with(csrf())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-Author-Id", authorId.value().toString())
                        .param("title", "Title")
                        .param("content", "")
                        .param("locale", "de"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeHasFieldErrors("postForm", "content"));

        verifyNoInteractions(postUseCase);
    }

    @Test
    @DisplayName("SWR-027: POST /posts returns form with errors on blank locale")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createPost_blankLocale_returnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/posts")
                        .with(csrf())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-Author-Id", authorId.value().toString())
                        .param("title", "Title")
                        .param("content", "Content")
                        .param("locale", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeHasFieldErrors("postForm", "locale"));

        verifyNoInteractions(postUseCase);
    }

    @Test
    @DisplayName("SWR-027: GET /posts/{id}/edit returns pre-filled form")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void editPostForm_returnsPreFilledForm() throws Exception {
        UUID postId = UUID.randomUUID();
        Post post =
                Post.create(TenantId.of(tenantId), authorId, "Existing Post", "Existing content", PostLocale.german());
        when(postUseCase.getPost(any(PostId.class), any(TenantId.class))).thenReturn(post);

        mockMvc.perform(get("/posts/{id}/edit", postId).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeExists("postForm"))
                .andExpect(model().attribute("editMode", true))
                .andExpect(model().attribute("postId", postId));
    }

    @Test
    @DisplayName("SWR-027: POST /posts/{id} updates post and redirects on valid input")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updatePost_validInput_redirects() throws Exception {
        UUID postId = UUID.randomUUID();
        Post post =
                Post.create(TenantId.of(tenantId), authorId, "Updated Post", "Updated content", PostLocale.german());
        when(postUseCase.updatePost(any(UpdatePostCommand.class))).thenReturn(post);

        mockMvc.perform(post("/posts/{id}", postId)
                        .with(csrf())
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("title", "Updated Post")
                        .param("content", "Updated content")
                        .param("locale", "de"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        verify(postUseCase).updatePost(any(UpdatePostCommand.class));
    }

    @Test
    @DisplayName("SWR-027: POST /posts/{id} returns form with errors on blank title")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updatePost_blankTitle_returnsFormWithErrors() throws Exception {
        UUID postId = UUID.randomUUID();

        mockMvc.perform(post("/posts/{id}", postId)
                        .with(csrf())
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("title", "")
                        .param("content", "Content")
                        .param("locale", "de"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeHasFieldErrors("postForm", "title"))
                .andExpect(model().attribute("editMode", true))
                .andExpect(model().attribute("postId", postId));

        verifyNoInteractions(postUseCase);
    }

    @Test
    @DisplayName("SWR-002: POST /posts/{id}/publish publishes post and redirects")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void publishPost_redirects() throws Exception {
        UUID postId = UUID.randomUUID();

        mockMvc.perform(post("/posts/{id}/publish", postId).with(csrf()).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        verify(postUseCase).publishPost(any(PostId.class), any(TenantId.class));
    }
}
