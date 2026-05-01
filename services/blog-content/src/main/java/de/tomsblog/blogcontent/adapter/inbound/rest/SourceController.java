package de.tomsblog.blogcontent.adapter.inbound.rest;

import de.tomsblog.blogcontent.application.port.inbound.AddSourceCommand;
import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.application.port.inbound.RemoveSourceCommand;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.Source;
import de.tomsblog.shared.tenant.TenantId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST adapter for source management on posts.
 *
 * @req SWR-012
 */
@RestController
@RequestMapping("/api/posts/{postId}/sources")
@Tag(name = "Sources", description = "Reference source management for blog posts")
public class SourceController {

    private final PostUseCase postUseCase;

    public SourceController(PostUseCase postUseCase) {
        this.postUseCase = postUseCase;
    }

    @PostMapping
    @Operation(summary = "Add a source to a post")
    @ApiResponse(responseCode = "200", description = "Source added")
    @ApiResponse(responseCode = "404", description = "Post not found")
    public ResponseEntity<SourceResponse> addSource(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID postId,
            @Valid @RequestBody AddSourceRequest request) {
        AddSourceCommand command =
                new AddSourceCommand(PostId.of(postId), TenantId.of(tenantId), request.url(), request.title());
        postUseCase.addSource(command);
        return ResponseEntity.ok(new SourceResponse(request.url(), request.title()));
    }

    @GetMapping
    @Operation(summary = "List all sources for a post")
    @ApiResponse(responseCode = "200", description = "List of sources")
    @ApiResponse(responseCode = "404", description = "Post not found")
    public ResponseEntity<List<SourceResponse>> listSources(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID postId) {
        List<Source> sources = postUseCase.listSources(PostId.of(postId), TenantId.of(tenantId));
        List<SourceResponse> responses =
                sources.stream().map(SourceResponse::from).toList();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping
    @Operation(summary = "Remove a source from a post")
    @ApiResponse(responseCode = "204", description = "Source removed")
    @ApiResponse(responseCode = "404", description = "Post not found")
    public ResponseEntity<Void> removeSource(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID postId,
            @Valid @RequestBody RemoveSourceRequest request) {
        RemoveSourceCommand command =
                new RemoveSourceCommand(PostId.of(postId), TenantId.of(tenantId), request.url(), request.title());
        postUseCase.removeSource(command);
        return ResponseEntity.noContent().build();
    }
}
