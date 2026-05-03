package de.tomsblog.blogcontent.adapter.inbound.rest;

import de.tomsblog.blogcontent.application.port.inbound.CreatePostCommand;
import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.application.port.inbound.UpdatePostCommand;
import de.tomsblog.blogcontent.domain.model.Post;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.shared.domain.AuthorId;
import de.tomsblog.shared.tenant.TenantId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST adapter for post management.
 *
 * @req SWR-001
 * @req SWR-002
 * @req SWR-015
 * @req SWR-038
 */
@RestController
@RequestMapping("/api/posts")
@Tag(name = "Posts", description = "Blog post CRUD and lifecycle operations")
@SuppressWarnings("null")
public class PostController {

    private final PostUseCase postUseCase;

    public PostController(PostUseCase postUseCase) {
        this.postUseCase = postUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new blog post", description = "Creates a draft blog post for the given tenant.")
    @ApiResponse(responseCode = "201", description = "Post created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    public ResponseEntity<PostResponse> createPost(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody CreatePostRequest request) {
        CreatePostCommand command = new CreatePostCommand(
                TenantId.of(tenantId),
                AuthorId.of(request.authorId()),
                request.title(),
                request.content(),
                request.contentType(),
                request.locale(),
                request.socialMediaTitle(),
                request.socialMediaSummary(),
                request.seriesPreviousPostId(),
                request.seriesNextPostId());
        Post post = postUseCase.createPost(command);
        PostResponse response = PostResponse.from(post);
        URI location = URI.create("/api/posts/" + post.getId().asString());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a blog post by ID")
    @ApiResponse(responseCode = "200", description = "Post found")
    @ApiResponse(responseCode = "404", description = "Post not found")
    public ResponseEntity<PostResponse> getPost(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID id) {
        Post post = postUseCase.getPost(PostId.of(id), TenantId.of(tenantId));
        return ResponseEntity.ok(PostResponse.from(post));
    }

    @GetMapping
    @Operation(summary = "List all posts for a tenant")
    @ApiResponse(responseCode = "200", description = "List of posts")
    public ResponseEntity<List<PostResponse>> listPosts(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId) {
        List<Post> posts = postUseCase.listPosts(TenantId.of(tenantId));
        List<PostResponse> responses = posts.stream().map(PostResponse::from).toList();
        return ResponseEntity.ok(responses);
    }

    /** @req SWR-038 */
    @GetMapping("/search")
    @Operation(summary = "Search published posts", description = "Full-text search over published posts of a tenant.")
    @ApiResponse(responseCode = "200", description = "Search results")
    public ResponseEntity<List<PostResponse>> searchPosts(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(name = "q", required = false, defaultValue = "") String query) {
        List<Post> posts = postUseCase.searchPublishedPosts(query, TenantId.of(tenantId));
        List<PostResponse> responses = posts.stream().map(PostResponse::from).toList();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a blog post", description = "Updates title, content, and social media fields.")
    @ApiResponse(responseCode = "200", description = "Post updated")
    @ApiResponse(responseCode = "404", description = "Post not found")
    public ResponseEntity<PostResponse> updatePost(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePostRequest request) {
        UpdatePostCommand command = new UpdatePostCommand(
                PostId.of(id),
                TenantId.of(tenantId),
                request.title(),
                request.content(),
                request.socialMediaTitle(),
                request.socialMediaSummary(),
                request.seriesPreviousPostId(),
                request.seriesNextPostId());
        Post post = postUseCase.updatePost(command);
        return ResponseEntity.ok(PostResponse.from(post));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a blog post", description = "Transitions the post from DRAFT to PUBLISHED status.")
    @ApiResponse(responseCode = "200", description = "Post published")
    @ApiResponse(responseCode = "404", description = "Post not found")
    @ApiResponse(responseCode = "409", description = "Post is already published or archived")
    public ResponseEntity<Void> publishPost(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID id) {
        postUseCase.publishPost(PostId.of(id), TenantId.of(tenantId));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a blog post")
    @ApiResponse(responseCode = "204", description = "Post deleted")
    public ResponseEntity<Void> deletePost(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID id) {
        postUseCase.deletePost(PostId.of(id), TenantId.of(tenantId));
        return ResponseEntity.noContent().build();
    }
}
