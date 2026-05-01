package de.tomsblog.blogcontent.adapter.inbound.rest;

import de.tomsblog.blogcontent.application.port.inbound.AddSourceCommand;
import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.application.port.inbound.RemoveSourceCommand;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.Source;
import de.tomsblog.shared.tenant.TenantId;
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
public class SourceController {

    private final PostUseCase postUseCase;

    public SourceController(PostUseCase postUseCase) {
        this.postUseCase = postUseCase;
    }

    @PostMapping
    public ResponseEntity<SourceResponse> addSource(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID postId,
            @Valid @RequestBody AddSourceRequest request) {
        AddSourceCommand command =
                new AddSourceCommand(PostId.of(postId), TenantId.of(tenantId), request.url(), request.title());
        postUseCase.addSource(command);
        return ResponseEntity.ok(new SourceResponse(request.url(), request.title()));
    }

    @GetMapping
    public ResponseEntity<List<SourceResponse>> listSources(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID postId) {
        List<Source> sources = postUseCase.listSources(PostId.of(postId), TenantId.of(tenantId));
        List<SourceResponse> responses = sources.stream().map(SourceResponse::from).toList();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping
    public ResponseEntity<Void> removeSource(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID postId,
            @Valid @RequestBody RemoveSourceRequest request) {
        RemoveSourceCommand command =
                new RemoveSourceCommand(PostId.of(postId), TenantId.of(tenantId), request.url(), request.title());
        postUseCase.removeSource(command);
        return ResponseEntity.noContent().build();
    }
}
