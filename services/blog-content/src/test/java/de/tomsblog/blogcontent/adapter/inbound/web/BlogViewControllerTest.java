package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import de.tomsblog.blogcontent.application.port.inbound.CreatePostCommand;
import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.application.port.inbound.TagUseCase;
import de.tomsblog.blogcontent.application.port.inbound.UpdatePostCommand;
import de.tomsblog.blogcontent.application.port.outbound.MarkdownRenderer;
import de.tomsblog.blogcontent.application.service.PostNotFoundException;
import de.tomsblog.blogcontent.domain.model.*;
import de.tomsblog.shared.domain.AuthorId;
import de.tomsblog.shared.tenant.TenantId;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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

    @MockitoBean
    private TagUseCase tagUseCase;

    @MockitoBean
    private MarkdownRenderer markdownRenderer;

    @MockitoBean
    private UserManagementClient userManagementClient;

    private final UUID tenantId = UUID.randomUUID();
    private final AuthorId authorId = AuthorId.generate();

    @BeforeEach
    void setUp() {
        when(postUseCase.listFeaturedPosts(any(TenantId.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(postUseCase.listPosts(any(TenantId.class))).thenReturn(List.of());
        when(tagUseCase.listTags(any(TenantId.class))).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("SWR-033: GET / returns post list view with max 3 recent posts")
    void index_returnsPostListView() throws Exception {
        when(postUseCase.listRecentPublishedPosts(any(TenantId.class), eq(3))).thenReturn(List.of());

        mockMvc.perform(get("/").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"))
                .andExpect(model().attributeExists("posts"))
                .andExpect(model().attributeExists("excerpts"))
                .andExpect(model().attribute("landingPage", true));
    }

    @Test
    @DisplayName("SWR-037: GET / extracts first paragraph as rendered excerpt")
    void index_extractsFirstParagraphAsExcerpt() throws Exception {
        Post post = Post.create(
                TenantId.of(tenantId), authorId, "HTML Post", "<h1>Title</h1><p>Hello world</p>", PostLocale.german());
        post.publish();
        when(postUseCase.listRecentPublishedPosts(any(TenantId.class), eq(3))).thenReturn(List.of(post));

        mockMvc.perform(get("/").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute(
                                "excerpts",
                                org.hamcrest.Matchers.hasEntry(
                                        org.hamcrest.Matchers.equalTo(
                                                post.getId().value()),
                                        org.hamcrest.Matchers.equalTo("<p>Hello world</p>"))));
    }

    @Test
    @DisplayName("SWR-036: GET / returns full content as excerpt when no paragraph found")
    void index_returnsFullContentWhenNoParagraph() throws Exception {
        String content = "<div>No paragraph here</div>";
        Post post = Post.create(TenantId.of(tenantId), authorId, "Long Post", content, PostLocale.german());
        post.publish();
        when(postUseCase.listRecentPublishedPosts(any(TenantId.class), eq(3))).thenReturn(List.of(post));

        mockMvc.perform(get("/").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute(
                                "excerpts",
                                org.hamcrest.Matchers.hasEntry(
                                        org.hamcrest.Matchers.equalTo(
                                                post.getId().value()),
                                        org.hamcrest.Matchers.equalTo(content))));
    }

    @Test
    @DisplayName("SWR-033: GET / with auth returns at most 3 posts")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void index_authenticated_returnsAllPosts() throws Exception {
        Post post = Post.create(TenantId.of(tenantId), authorId, "Draft", "Content", PostLocale.german());
        when(postUseCase.listPosts(any(TenantId.class))).thenReturn(List.of(post));

        mockMvc.perform(get("/").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"));

        verify(postUseCase).listPosts(any(TenantId.class));
        verify(postUseCase, never()).listRecentPublishedPosts(any(TenantId.class), anyInt());
    }

    @Test
    @DisplayName("SWR-033: GET / with auth truncates list when more than 3 posts")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void index_authenticated_truncatesWhenMoreThanThree() throws Exception {
        Post p1 = Post.create(TenantId.of(tenantId), authorId, "Post One", "<p>C1</p>", PostLocale.german());
        Post p2 = Post.create(TenantId.of(tenantId), authorId, "Post Two", "<p>C2</p>", PostLocale.german());
        Post p3 = Post.create(TenantId.of(tenantId), authorId, "Post Three", "<p>C3</p>", PostLocale.german());
        Post p4 = Post.create(TenantId.of(tenantId), authorId, "Post Four", "<p>C4</p>", PostLocale.german());
        when(postUseCase.listPosts(any(TenantId.class))).thenReturn(List.of(p1, p2, p3, p4));

        mockMvc.perform(get("/").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("posts", org.hamcrest.Matchers.hasSize(3)));
    }

    @Test
    @DisplayName("SWR-035: GET /posts/{slug} renders markdown content")
    void showPost_rendersMarkdownContent() throws Exception {
        Post post = Post.create(
                TenantId.of(tenantId), authorId, "MD Post", "# Hello", ContentType.MARKDOWN, PostLocale.german());
        post.publish();
        when(postUseCase.getPublishedPostBySlug(any(Slug.class), any(TenantId.class)))
                .thenReturn(post);
        when(markdownRenderer.renderToHtml("# Hello")).thenReturn("<h1>Hello</h1>");
        when(postUseCase.findPreviousPublishedPost(any(Slug.class), any(TenantId.class)))
                .thenReturn(Optional.empty());
        when(postUseCase.findNextPublishedPost(any(Slug.class), any(TenantId.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/posts/md-post").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("renderedContent", "<h1>Hello</h1>"));

        verify(markdownRenderer).renderToHtml("# Hello");
    }

    @Test
    @DisplayName("SWR-036: Excerpt for blank content returns empty string")
    void index_blankContent_returnsEmptyExcerpt() throws Exception {
        Post post = Post.create(TenantId.of(tenantId), authorId, "Empty Post", "", PostLocale.german());
        post.publish();
        when(postUseCase.listRecentPublishedPosts(any(TenantId.class), eq(3))).thenReturn(List.of(post));

        mockMvc.perform(get("/").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute(
                                "excerpts",
                                org.hamcrest.Matchers.hasEntry(
                                        org.hamcrest.Matchers.equalTo(
                                                post.getId().value()),
                                        org.hamcrest.Matchers.equalTo(""))));
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
    @DisplayName("SWR-026: GET /posts/{slug} returns post detail view with rendered content")
    void showPost_returnsShowView() throws Exception {
        Post post = Post.create(TenantId.of(tenantId), authorId, "My Post", "Full content here", PostLocale.german());
        post.publish();
        when(postUseCase.getPublishedPostBySlug(any(Slug.class), any(TenantId.class)))
                .thenReturn(post);
        when(postUseCase.findPreviousPublishedPost(any(Slug.class), any(TenantId.class)))
                .thenReturn(Optional.empty());
        when(postUseCase.findNextPublishedPost(any(Slug.class), any(TenantId.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/posts/my-post").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/show"))
                .andExpect(model().attributeExists("post"))
                .andExpect(model().attributeExists("renderedContent"));
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
    @DisplayName("SWR-039: GET /posts/{slug} includes previous and next post navigation")
    void showPost_includesChronologicalNavigation() throws Exception {
        Post current = Post.create(TenantId.of(tenantId), authorId, "Current Post", "Content", PostLocale.german());
        current.publish();
        Post previous = Post.create(TenantId.of(tenantId), authorId, "Previous Post", "Content", PostLocale.german());
        previous.publish();
        Post next = Post.create(TenantId.of(tenantId), authorId, "Next Post", "Content", PostLocale.german());
        next.publish();

        when(postUseCase.getPublishedPostBySlug(any(Slug.class), any(TenantId.class)))
                .thenReturn(current);
        when(postUseCase.findPreviousPublishedPost(any(Slug.class), any(TenantId.class)))
                .thenReturn(Optional.of(previous));
        when(postUseCase.findNextPublishedPost(any(Slug.class), any(TenantId.class)))
                .thenReturn(Optional.of(next));

        mockMvc.perform(get("/posts/current-post").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("previousPost"))
                .andExpect(model().attributeExists("nextPost"))
                .andExpect(model().attributeExists("previousPostExcerpt"))
                .andExpect(model().attributeExists("nextPostExcerpt"));
    }

    @Test
    @DisplayName("SWR-038: GET /posts?q=search returns search results")
    void listPosts_withSearchQuery_returnsSearchResults() throws Exception {
        Post post = Post.create(TenantId.of(tenantId), authorId, "Found Post", "Content", PostLocale.german());
        post.publish();
        when(postUseCase.searchPublishedPosts(eq("found"), any(TenantId.class))).thenReturn(List.of(post));

        mockMvc.perform(get("/posts").param("q", "found").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"))
                .andExpect(model().attribute("searchQuery", "found"))
                .andExpect(model().attribute("posts", org.hamcrest.Matchers.hasSize(1)));

        verify(postUseCase).searchPublishedPosts(eq("found"), any(TenantId.class));
    }

    @Test
    @DisplayName("SWR-027: GET /posts/new returns empty form")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void newPostForm_returnsEmptyForm() throws Exception {
        mockMvc.perform(get("/posts/new").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeExists("postForm"))
                .andExpect(model().attributeExists("availablePosts"))
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
                        .param("contentType", "HTML")
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
                        .param("contentType", "HTML")
                        .param("locale", "de"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeHasFieldErrors("postForm", "title"))
                .andExpect(model().attribute("editMode", false));

        verify(postUseCase, never()).createPost(any(CreatePostCommand.class));
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
                        .param("contentType", "HTML")
                        .param("locale", "de"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeHasFieldErrors("postForm", "content"));

        verify(postUseCase, never()).createPost(any(CreatePostCommand.class));
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
                        .param("contentType", "HTML")
                        .param("locale", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeHasFieldErrors("postForm", "locale"));

        verify(postUseCase, never()).createPost(any(CreatePostCommand.class));
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
                        .param("contentType", "HTML")
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
                        .param("contentType", "HTML")
                        .param("locale", "de"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeHasFieldErrors("postForm", "title"))
                .andExpect(model().attribute("editMode", true))
                .andExpect(model().attribute("postId", postId));

        verify(postUseCase, never()).updatePost(any(UpdatePostCommand.class));
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

    @Test
    @DisplayName("SWR-040: GET /posts/{slug} shows series navigation when different from chronological")
    void showPost_showsSeriesNavigation() throws Exception {
        PostId seriesPrevId = PostId.generate();
        PostId seriesNextId = PostId.generate();
        Post current = Post.reconstitute(
                PostId.generate(),
                TenantId.of(tenantId),
                authorId,
                "Series Post",
                Slug.fromTitle("Series Post"),
                "Content",
                ContentType.HTML,
                PostStatus.PUBLISHED,
                PostLocale.german(),
                java.util.Set.of(),
                List.of(),
                List.of(),
                java.time.Instant.now(),
                null,
                null,
                seriesPrevId,
                seriesNextId,
                null,
                null);

        Post seriesPrev = Post.create(TenantId.of(tenantId), authorId, "Series Prev", "Content", PostLocale.german());
        seriesPrev.publish();
        Post seriesNext = Post.create(TenantId.of(tenantId), authorId, "Series Next", "Content", PostLocale.german());
        seriesNext.publish();

        when(postUseCase.getPublishedPostBySlug(any(Slug.class), any(TenantId.class)))
                .thenReturn(current);
        when(postUseCase.findPreviousPublishedPost(any(Slug.class), any(TenantId.class)))
                .thenReturn(Optional.empty());
        when(postUseCase.findNextPublishedPost(any(Slug.class), any(TenantId.class)))
                .thenReturn(Optional.empty());
        when(postUseCase.getPostIfPublished(eq(seriesPrevId), any(TenantId.class)))
                .thenReturn(Optional.of(seriesPrev));
        when(postUseCase.getPostIfPublished(eq(seriesNextId), any(TenantId.class)))
                .thenReturn(Optional.of(seriesNext));

        mockMvc.perform(get("/posts/series-post").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("seriesPreviousPost"))
                .andExpect(model().attributeExists("seriesNextPost"));
    }

    @Test
    @DisplayName("SWR-040: GET /posts/{slug} hides series navigation when same as chronological")
    void showPost_hidesSeriesNavigationWhenSameAsChronological() throws Exception {
        Post chronoPrev = Post.create(TenantId.of(tenantId), authorId, "Chrono Prev", "Content", PostLocale.german());
        chronoPrev.publish();

        Post current = Post.reconstitute(
                PostId.generate(),
                TenantId.of(tenantId),
                authorId,
                "Current",
                Slug.fromTitle("Current"),
                "Content",
                ContentType.HTML,
                PostStatus.PUBLISHED,
                PostLocale.german(),
                java.util.Set.of(),
                List.of(),
                List.of(),
                java.time.Instant.now(),
                null,
                null,
                chronoPrev.getId(),
                null,
                null,
                null);

        when(postUseCase.getPublishedPostBySlug(any(Slug.class), any(TenantId.class)))
                .thenReturn(current);
        when(postUseCase.findPreviousPublishedPost(any(Slug.class), any(TenantId.class)))
                .thenReturn(Optional.of(chronoPrev));
        when(postUseCase.findNextPublishedPost(any(Slug.class), any(TenantId.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/posts/current").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("seriesPreviousPost"));

        verify(postUseCase, never()).getPostIfPublished(any(), any());
    }

    @Test
    @DisplayName("SWR-038: GET /posts?q= with empty search returns all published (anonymous)")
    void listPosts_emptySearch_returnsAllPublished() throws Exception {
        when(postUseCase.listPublishedPosts(any(TenantId.class))).thenReturn(List.of());

        mockMvc.perform(get("/posts").param("q", "").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("searchQuery"));

        verify(postUseCase).listPublishedPosts(any(TenantId.class));
    }

    @Test
    @DisplayName("SWR-040: POST /posts with series fields passes them to command")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createPost_withSeriesFields() throws Exception {
        UUID prevId = UUID.randomUUID();
        Post post = Post.create(TenantId.of(tenantId), authorId, "New Post", "Content", PostLocale.german());
        when(postUseCase.createPost(any(CreatePostCommand.class))).thenReturn(post);

        mockMvc.perform(post("/posts")
                        .with(csrf())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-Author-Id", authorId.value().toString())
                        .param("title", "New Post")
                        .param("content", "Some content")
                        .param("contentType", "HTML")
                        .param("locale", "de")
                        .param("seriesPreviousPostId", prevId.toString()))
                .andExpect(status().is3xxRedirection());

        verify(postUseCase).createPost(any(CreatePostCommand.class));
    }

    @Test
    @DisplayName("SWR-040: POST /posts/{id} with series fields passes them to command")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updatePost_withSeriesFields() throws Exception {
        UUID postId = UUID.randomUUID();
        UUID nextId = UUID.randomUUID();
        Post post = Post.create(TenantId.of(tenantId), authorId, "Updated", "Content", PostLocale.german());
        when(postUseCase.updatePost(any(UpdatePostCommand.class))).thenReturn(post);

        mockMvc.perform(post("/posts/{id}", postId)
                        .with(csrf())
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("title", "Updated")
                        .param("content", "Content")
                        .param("contentType", "HTML")
                        .param("locale", "de")
                        .param("seriesNextPostId", nextId.toString()))
                .andExpect(status().is3xxRedirection());

        verify(postUseCase).updatePost(any(UpdatePostCommand.class));
    }

    @Test
    @DisplayName("SWR-040: GET /posts/{id}/edit pre-fills series fields")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void editPostForm_prefillsSeriesFields() throws Exception {
        UUID postId = UUID.randomUUID();
        UUID prevId = UUID.randomUUID();
        UUID nextId = UUID.randomUUID();
        Post post = Post.reconstitute(
                PostId.of(postId),
                TenantId.of(tenantId),
                authorId,
                "Series Post",
                Slug.fromTitle("Series Post"),
                "Content",
                ContentType.HTML,
                PostStatus.DRAFT,
                PostLocale.german(),
                java.util.Set.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                PostId.of(prevId),
                PostId.of(nextId),
                null,
                null);
        when(postUseCase.getPost(any(PostId.class), any(TenantId.class))).thenReturn(post);

        mockMvc.perform(get("/posts/{id}/edit", postId).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeExists("postForm"));
    }

    @Test
    @DisplayName("SWR-041: GET /posts/{id}/edit pre-fills featured fields")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void editPostForm_prefillsFeaturedFields() throws Exception {
        UUID postId = UUID.randomUUID();
        Post post = Post.reconstitute(
                PostId.of(postId),
                TenantId.of(tenantId),
                authorId,
                "Featured Post",
                Slug.fromTitle("Featured Post"),
                "Content",
                ContentType.HTML,
                PostStatus.DRAFT,
                PostLocale.german(),
                java.util.Set.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31));
        when(postUseCase.getPost(any(PostId.class), any(TenantId.class))).thenReturn(post);

        mockMvc.perform(get("/posts/{id}/edit", postId).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeExists("postForm"));
    }

    @Test
    @DisplayName("SWR-041: POST /posts passes featured dates to command")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createPost_withFeaturedDates() throws Exception {
        Post post = Post.create(TenantId.of(tenantId), authorId, "Featured", "Content", PostLocale.german());
        when(postUseCase.createPost(any(CreatePostCommand.class))).thenReturn(post);

        mockMvc.perform(post("/posts")
                        .with(csrf())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-Author-Id", authorId.value().toString())
                        .param("title", "Featured")
                        .param("content", "Content")
                        .param("contentType", "HTML")
                        .param("locale", "de")
                        .param("featuredFrom", "2026-05-01")
                        .param("featuredUntil", "2026-05-31"))
                .andExpect(status().is3xxRedirection());

        verify(postUseCase).createPost(any(CreatePostCommand.class));
    }

    @Test
    @DisplayName("SWR-040: Series next same as chrono next is suppressed")
    void showPost_seriesNextSameAsChronoNext_isSuppressed() throws Exception {
        Post chronoNext = Post.create(TenantId.of(tenantId), authorId, "Chrono Next", "Content", PostLocale.german());
        chronoNext.publish();

        Post current = Post.reconstitute(
                PostId.generate(),
                TenantId.of(tenantId),
                authorId,
                "Current",
                Slug.fromTitle("Current"),
                "Content",
                ContentType.HTML,
                PostStatus.PUBLISHED,
                PostLocale.german(),
                java.util.Set.of(),
                List.of(),
                List.of(),
                java.time.Instant.now(),
                null,
                null,
                null,
                chronoNext.getId(),
                null,
                null);

        when(postUseCase.getPublishedPostBySlug(any(Slug.class), any(TenantId.class)))
                .thenReturn(current);
        when(postUseCase.findPreviousPublishedPost(any(Slug.class), any(TenantId.class)))
                .thenReturn(Optional.empty());
        when(postUseCase.findNextPublishedPost(any(Slug.class), any(TenantId.class)))
                .thenReturn(Optional.of(chronoNext));

        mockMvc.perform(get("/posts/current").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("seriesNextPost"));

        verify(postUseCase, never()).getPostIfPublished(any(), any());
    }

    @Test
    @DisplayName("SWR-040: Series nav with getPostIfPublished returning empty")
    void showPost_seriesNavPostNotPublished_noSeriesInModel() throws Exception {
        PostId seriesPrevId = PostId.generate();

        Post current = Post.reconstitute(
                PostId.generate(),
                TenantId.of(tenantId),
                authorId,
                "Current",
                Slug.fromTitle("Current"),
                "Content",
                ContentType.HTML,
                PostStatus.PUBLISHED,
                PostLocale.german(),
                java.util.Set.of(),
                List.of(),
                List.of(),
                java.time.Instant.now(),
                null,
                null,
                seriesPrevId,
                null,
                null,
                null);

        when(postUseCase.getPublishedPostBySlug(any(Slug.class), any(TenantId.class)))
                .thenReturn(current);
        when(postUseCase.findPreviousPublishedPost(any(Slug.class), any(TenantId.class)))
                .thenReturn(Optional.empty());
        when(postUseCase.findNextPublishedPost(any(Slug.class), any(TenantId.class)))
                .thenReturn(Optional.empty());
        when(postUseCase.getPostIfPublished(eq(seriesPrevId), any(TenantId.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/posts/current").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("seriesPreviousPost"));
    }

    @Test
    @DisplayName("SWR-040: Series next different from chrono next is shown")
    void showPost_seriesNextDifferentFromChronoNext_isShown() throws Exception {
        Post chronoNext = Post.create(TenantId.of(tenantId), authorId, "Chrono Next", "Content", PostLocale.german());
        chronoNext.publish();

        PostId seriesNextId = PostId.generate();
        Post seriesNext = Post.create(TenantId.of(tenantId), authorId, "Series Next", "Content", PostLocale.german());
        seriesNext.publish();

        Post current = Post.reconstitute(
                PostId.generate(),
                TenantId.of(tenantId),
                authorId,
                "Current",
                Slug.fromTitle("Current"),
                "Content",
                ContentType.HTML,
                PostStatus.PUBLISHED,
                PostLocale.german(),
                java.util.Set.of(),
                List.of(),
                List.of(),
                java.time.Instant.now(),
                null,
                null,
                null,
                seriesNextId,
                null,
                null);

        when(postUseCase.getPublishedPostBySlug(any(Slug.class), any(TenantId.class)))
                .thenReturn(current);
        when(postUseCase.findPreviousPublishedPost(any(Slug.class), any(TenantId.class)))
                .thenReturn(Optional.empty());
        when(postUseCase.findNextPublishedPost(any(Slug.class), any(TenantId.class)))
                .thenReturn(Optional.of(chronoNext));
        when(postUseCase.getPostIfPublished(eq(seriesNextId), any(TenantId.class)))
                .thenReturn(Optional.of(seriesNext));

        mockMvc.perform(get("/posts/current").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("seriesNextPost"));
    }

    @Test
    @DisplayName("SWR-076: GET /posts/{id}/preview returns show view with preview flag for draft post")
    @WithMockUser(username = "author", roles = "AUTHOR")
    void previewPost_draftPost_returnsShowViewWithPreviewFlag() throws Exception {
        UUID postId = UUID.randomUUID();
        Post post = Post.create(TenantId.of(tenantId), authorId, "Draft Post", "Draft content", PostLocale.german());
        when(postUseCase.getPost(any(PostId.class), any(TenantId.class))).thenReturn(post);

        mockMvc.perform(get("/posts/{id}/preview", postId).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/show"))
                .andExpect(model().attributeExists("post"))
                .andExpect(model().attributeExists("renderedContent"))
                .andExpect(model().attribute("preview", true));
    }

    @Test
    @DisplayName("SWR-076: GET /posts/{id}/preview renders markdown content")
    @WithMockUser(username = "author", roles = "AUTHOR")
    void previewPost_markdownPost_rendersContent() throws Exception {
        UUID postId = UUID.randomUUID();
        Post post = Post.create(
                TenantId.of(tenantId), authorId, "MD Post", "# Hello", ContentType.MARKDOWN, PostLocale.german());
        when(postUseCase.getPost(any(PostId.class), any(TenantId.class))).thenReturn(post);
        when(markdownRenderer.renderToHtml("# Hello")).thenReturn("<h1>Hello</h1>");

        mockMvc.perform(get("/posts/{id}/preview", postId).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/show"))
                .andExpect(model().attribute("renderedContent", "<h1>Hello</h1>"));
    }

    @Test
    @DisplayName("SWR-076: GET /posts/{id}/preview returns 404 when post not found")
    @WithMockUser(username = "author", roles = "AUTHOR")
    void previewPost_notFound_returns404() throws Exception {
        UUID postId = UUID.randomUUID();
        when(postUseCase.getPost(any(PostId.class), any(TenantId.class)))
                .thenThrow(new PostNotFoundException(new PostId(postId)));

        mockMvc.perform(get("/posts/{id}/preview", postId).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SWR-076: GET /posts/{id}/preview requires authentication")
    void previewPost_unauthenticated_redirectsToLogin() throws Exception {
        UUID postId = UUID.randomUUID();

        mockMvc.perform(get("/posts/{id}/preview", postId).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("SWR-085: GET /posts/new includes available tags in model")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void newPostForm_includesAvailableTags() throws Exception {
        Tag tag = Tag.create(TenantId.of(tenantId), "Java");
        when(tagUseCase.listTags(any(TenantId.class))).thenReturn(List.of(tag));

        mockMvc.perform(get("/posts/new").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("availableTags"));
    }

    @Test
    @DisplayName("SWR-085: GET /posts/{id}/edit includes available tags in model")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void editPostForm_includesAvailableTags() throws Exception {
        UUID postId = UUID.randomUUID();
        Post post =
                Post.create(TenantId.of(tenantId), authorId, "Existing Post", "Existing content", PostLocale.german());
        when(postUseCase.getPost(any(PostId.class), any(TenantId.class))).thenReturn(post);

        Tag tag = Tag.create(TenantId.of(tenantId), "Spring");
        when(tagUseCase.listTags(any(TenantId.class))).thenReturn(List.of(tag));

        mockMvc.perform(get("/posts/{id}/edit", postId).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("availableTags"));
    }

    @Test
    @DisplayName("SWR-085: POST /posts syncs tags after creation")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createPost_withTags_syncsTags() throws Exception {
        Post post = Post.create(TenantId.of(tenantId), authorId, "New Post", "Content", PostLocale.german());
        when(postUseCase.createPost(any(CreatePostCommand.class))).thenReturn(post);
        when(postUseCase.syncPostTags(any(), any(), any())).thenReturn(post);

        UUID tagId = UUID.randomUUID();
        mockMvc.perform(post("/posts")
                        .with(csrf())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-Author-Id", authorId.value().toString())
                        .param("title", "New Post")
                        .param("content", "Some content")
                        .param("contentType", "HTML")
                        .param("locale", "de")
                        .param("tagIds", tagId.toString()))
                .andExpect(status().is3xxRedirection());

        verify(postUseCase).syncPostTags(any(), any(), any());
    }

    @Test
    @DisplayName("SWR-086: GET /posts/{slug} includes tags in model")
    void showPost_includesTags() throws Exception {
        Tag tag = Tag.create(TenantId.of(tenantId), "Java");
        TagId tagId = tag.getId();
        Post post = Post.reconstitute(
                PostId.generate(),
                TenantId.of(tenantId),
                authorId,
                "Tagged Post",
                Slug.fromTitle("Tagged Post"),
                "Content",
                ContentType.HTML,
                PostStatus.PUBLISHED,
                PostLocale.german(),
                java.util.Set.of(tagId),
                List.of(),
                List.of(),
                java.time.Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null);
        when(postUseCase.getPublishedPostBySlug(any(Slug.class), any(TenantId.class)))
                .thenReturn(post);
        when(postUseCase.findPreviousPublishedPost(any(Slug.class), any(TenantId.class)))
                .thenReturn(Optional.empty());
        when(postUseCase.findNextPublishedPost(any(Slug.class), any(TenantId.class)))
                .thenReturn(Optional.empty());
        when(tagUseCase.listTags(any(TenantId.class))).thenReturn(List.of(tag));

        mockMvc.perform(get("/posts/tagged-post").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("tags"));
    }

    @Test
    @DisplayName("SWR-086: GET /posts includes postTags map in model")
    void listPosts_includesPostTagsMap() throws Exception {
        Tag tag = Tag.create(TenantId.of(tenantId), "Java");
        TagId tagId = tag.getId();
        Post post = Post.reconstitute(
                PostId.generate(),
                TenantId.of(tenantId),
                authorId,
                "Tagged",
                Slug.fromTitle("Tagged"),
                "Content",
                ContentType.HTML,
                PostStatus.PUBLISHED,
                PostLocale.german(),
                java.util.Set.of(tagId),
                List.of(),
                List.of(),
                java.time.Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null);
        when(postUseCase.listPublishedPosts(any(TenantId.class))).thenReturn(List.of(post));
        when(tagUseCase.listTags(any(TenantId.class))).thenReturn(List.of(tag));

        mockMvc.perform(get("/posts").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("postTags"));
    }

    @Test
    @DisplayName("SWR-085: POST /posts creates new tag when newTagName is provided")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createPost_withNewTagName_createsTag() throws Exception {
        Post post = Post.create(TenantId.of(tenantId), authorId, "New Post", "Content", PostLocale.german());
        when(postUseCase.createPost(any(CreatePostCommand.class))).thenReturn(post);
        when(postUseCase.syncPostTags(any(), any(), any())).thenReturn(post);
        Tag newTag = Tag.create(TenantId.of(tenantId), "NewTag");
        when(tagUseCase.createTag(any())).thenReturn(newTag);

        mockMvc.perform(post("/posts")
                        .with(csrf())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-Author-Id", authorId.value().toString())
                        .param("title", "New Post")
                        .param("content", "Content")
                        .param("contentType", "HTML")
                        .param("locale", "de")
                        .param("newTagName", "NewTag"))
                .andExpect(status().is3xxRedirection());

        verify(tagUseCase).createTag(any());
        verify(postUseCase).syncPostTags(any(), any(), any());
    }

    @Test
    @DisplayName("SWR-085: POST /posts/{id} syncs tags after update")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updatePost_withTags_syncsTags() throws Exception {
        UUID postId = UUID.randomUUID();
        Post post = Post.create(TenantId.of(tenantId), authorId, "Updated", "Content", PostLocale.german());
        when(postUseCase.updatePost(any(UpdatePostCommand.class))).thenReturn(post);
        when(postUseCase.syncPostTags(any(), any(), any())).thenReturn(post);

        UUID tagId = UUID.randomUUID();
        mockMvc.perform(post("/posts/{id}", postId)
                        .with(csrf())
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("title", "Updated")
                        .param("content", "Content")
                        .param("contentType", "HTML")
                        .param("locale", "de")
                        .param("tagIds", tagId.toString()))
                .andExpect(status().is3xxRedirection());

        verify(postUseCase).syncPostTags(any(), any(), any());
    }
}
