package de.tomsblog.blogcontent.adapter.inbound.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.tomsblog.blogcontent.adapter.inbound.web.SecurityConfiguration;
import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.application.service.PostNotFoundException;
import de.tomsblog.blogcontent.domain.model.*;
import de.tomsblog.shared.domain.AuthorId;
import de.tomsblog.shared.tenant.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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

@WebMvcTest(PostController.class)
@Import(SecurityConfiguration.class)
@WithMockUser(username = "admin", roles = "ADMIN")
@SuppressWarnings("null")
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostUseCase postUseCase;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();

    @Test
    @DisplayName("SWR-001: POST /api/posts creates post and returns 201")
    void createPost_returns201() throws Exception {
        Post post = Post.create(
                TenantId.of(tenantId), AuthorId.of(authorId), "Test Post", "Test Content", PostLocale.german());
        when(postUseCase.createPost(any())).thenReturn(post);

        String body = """
                {
                    "authorId": "%s",
                    "title": "Test Post",
                    "content": "Test Content"
                }
                """.formatted(authorId);

        mockMvc.perform(post("/api/posts")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.title").value("Test Post"))
                .andExpect(jsonPath("$.slug").value("test-post"));
    }

    @Test
    @DisplayName("SWR-001: GET /api/posts/{id} returns post")
    void getPost_returns200() throws Exception {
        Post post =
                Post.create(TenantId.of(tenantId), AuthorId.of(authorId), "My Post", "Content", PostLocale.german());
        when(postUseCase.getPost(any(), any())).thenReturn(post);

        mockMvc.perform(get("/api/posts/{id}", post.getId().value()).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My Post"));
    }

    @Test
    @DisplayName("SWR-001: GET /api/posts/{id} includes sources and attachments in response")
    void getPost_returnsSourcesAndAttachments() throws Exception {
        Post post = Post.reconstitute(
                PostId.generate(),
                TenantId.of(tenantId),
                AuthorId.of(authorId),
                "Rich Post",
                Slug.fromTitle("Rich Post"),
                "Content with references",
                PostStatus.PUBLISHED,
                PostLocale.german(),
                Set.of(),
                List.of(new Source("https://example.com", "Example Site")),
                List.of(new Attachment(
                        AttachmentId.generate(), "photo.jpg", "image/jpeg", 4096, true, "s3://bucket/photo.jpg")),
                Instant.now(),
                null,
                null);
        when(postUseCase.getPost(any(), any())).thenReturn(post);

        mockMvc.perform(get("/api/posts/{id}", post.getId().value()).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sources[0].url").value("https://example.com"))
                .andExpect(jsonPath("$.sources[0].title").value("Example Site"))
                .andExpect(jsonPath("$.attachments[0].filename").value("photo.jpg"))
                .andExpect(jsonPath("$.attachments[0].contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.attachments[0].size").value(4096))
                .andExpect(jsonPath("$.attachments[0].show").value(true));
    }

    @Test
    @DisplayName("SWR-001: GET /api/posts/{id} returns 404 when not found")
    void getPost_returns404WhenNotFound() throws Exception {
        PostId postId = PostId.generate();
        when(postUseCase.getPost(any(), any())).thenThrow(new PostNotFoundException(postId));

        mockMvc.perform(get("/api/posts/{id}", postId.value()).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SWR-009: GET /api/posts returns list of posts")
    void listPosts_returns200() throws Exception {
        Post post1 =
                Post.create(TenantId.of(tenantId), AuthorId.of(authorId), "Post One", "Content 1", PostLocale.german());
        Post post2 =
                Post.create(TenantId.of(tenantId), AuthorId.of(authorId), "Post Two", "Content 2", PostLocale.german());
        when(postUseCase.listPosts(any())).thenReturn(List.of(post1, post2));

        mockMvc.perform(get("/api/posts").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("SWR-002: POST /api/posts/{id}/publish returns 200")
    void publishPost_returns200() throws Exception {
        UUID postId = UUID.randomUUID();
        doNothing().when(postUseCase).publishPost(any(), any());

        mockMvc.perform(post("/api/posts/{id}/publish", postId).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/posts with missing title returns 400")
    void createPost_returns400WhenTitleMissing() throws Exception {
        String body = """
                {
                    "authorId": "%s",
                    "title": "",
                    "content": "Content"
                }
                """.formatted(authorId);

        mockMvc.perform(post("/api/posts")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/posts/{id} updates post and returns 200")
    void updatePost_returns200() throws Exception {
        Post post = Post.create(
                TenantId.of(tenantId), AuthorId.of(authorId), "Updated", "New Content", PostLocale.german());
        when(postUseCase.updatePost(any())).thenReturn(post);

        UUID postId = UUID.randomUUID();
        String body = """
                {
                    "title": "Updated",
                    "content": "New Content",
                    "socialMediaTitle": "SM Title",
                    "socialMediaSummary": "SM Summary"
                }
                """;

        mockMvc.perform(put("/api/posts/{id}", postId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    @DisplayName("DELETE /api/posts/{id} returns 204")
    void deletePost_returns204() throws Exception {
        UUID postId = UUID.randomUUID();
        doNothing().when(postUseCase).deletePost(any(), any());

        mockMvc.perform(delete("/api/posts/{id}", postId).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/posts/{id}/publish returns 409 on IllegalStateException")
    void publishPost_returns409OnConflict() throws Exception {
        UUID postId = UUID.randomUUID();
        doThrow(new IllegalStateException("Post is already published"))
                .when(postUseCase)
                .publishPost(any(), any());

        mockMvc.perform(post("/api/posts/{id}/publish", postId).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/posts returns 400 on IllegalArgumentException")
    void createPost_returns400OnIllegalArgument() throws Exception {
        when(postUseCase.createPost(any())).thenThrow(new IllegalArgumentException("Invalid locale"));

        String body = """
                {
                    "authorId": "%s",
                    "title": "Test",
                    "content": "Content"
                }
                """.formatted(authorId);

        mockMvc.perform(post("/api/posts")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
