package de.tomsblog.blogcontent.adapter.inbound.rest;

import de.tomsblog.blogcontent.application.port.inbound.CreatePostCommand;
import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.application.port.inbound.UpdatePostCommand;
import de.tomsblog.blogcontent.domain.model.Post;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.shared.domain.AuthorId;
import de.tomsblog.shared.tenant.TenantId;
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
 */
@RestController
@RequestMapping("/api/posts")
@SuppressWarnings("null")
public class PostController {

    private final PostUseCase postUseCase;

    public PostController(PostUseCase postUseCase) {
        this.postUseCase = postUseCase;
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @Valid @RequestBody CreatePostRequest request) {
        CreatePostCommand command = new CreatePostCommand(
                TenantId.of(tenantId),
                AuthorId.of(request.authorId()),
                request.title(),
                request.content(),
                request.locale(),
                request.socialMediaTitle(),
                request.socialMediaSummary());
        Post post = postUseCase.createPost(command);
        PostResponse response = PostResponse.from(post);
        URI location = URI.create("/api/posts/" + post.getId().asString());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID id) {
        Post post = postUseCase.getPost(PostId.of(id), TenantId.of(tenantId));
        return ResponseEntity.ok(PostResponse.from(post));
    }

    @GetMapping
    public ResponseEntity<List<PostResponse>> listPosts(@RequestHeader("X-Tenant-Id") UUID tenantId) {
        List<Post> posts = postUseCase.listPosts(TenantId.of(tenantId));
        List<PostResponse> responses = posts.stream().map(PostResponse::from).toList();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePostRequest request) {
        UpdatePostCommand command = new UpdatePostCommand(
                PostId.of(id),
                TenantId.of(tenantId),
                request.title(),
                request.content(),
                request.socialMediaTitle(),
                request.socialMediaSummary());
        Post post = postUseCase.updatePost(command);
        return ResponseEntity.ok(PostResponse.from(post));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publishPost(@RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID id) {
        postUseCase.publishPost(PostId.of(id), TenantId.of(tenantId));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID id) {
        postUseCase.deletePost(PostId.of(id), TenantId.of(tenantId));
        return ResponseEntity.noContent().build();
    }
}
