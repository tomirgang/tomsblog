package de.tomsblog.blogcontent.adapter.inbound.rest;

import de.tomsblog.blogcontent.application.port.inbound.CreateTagCommand;
import de.tomsblog.blogcontent.application.port.inbound.RenameTagCommand;
import de.tomsblog.blogcontent.application.port.inbound.TagUseCase;
import de.tomsblog.blogcontent.domain.model.Tag;
import de.tomsblog.blogcontent.domain.model.TagId;
import de.tomsblog.shared.tenant.TenantId;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tags")
@SuppressWarnings("null")
public class TagController {

    private final TagUseCase tagUseCase;

    public TagController(TagUseCase tagUseCase) {
        this.tagUseCase = tagUseCase;
    }

    @PostMapping
    public ResponseEntity<TagResponse> createTag(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @Valid @RequestBody CreateTagRequest request) {
        CreateTagCommand command = new CreateTagCommand(TenantId.of(tenantId), request.name());
        Tag tag = tagUseCase.createTag(command);
        return ResponseEntity.created(URI.create("/api/tags/" + tag.getId().value()))
                .body(TagResponse.from(tag));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TagResponse> getTag(@RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID id) {
        Tag tag = tagUseCase.getTag(TagId.of(id), TenantId.of(tenantId));
        return ResponseEntity.ok(TagResponse.from(tag));
    }

    @GetMapping
    public ResponseEntity<List<TagResponse>> listTags(@RequestHeader("X-Tenant-Id") UUID tenantId) {
        List<Tag> tags = tagUseCase.listTags(TenantId.of(tenantId));
        List<TagResponse> response = tags.stream().map(TagResponse::from).toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TagResponse> renameTag(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody RenameTagRequest request) {
        RenameTagCommand command = new RenameTagCommand(TagId.of(id), TenantId.of(tenantId), request.name());
        Tag tag = tagUseCase.renameTag(command);
        return ResponseEntity.ok(TagResponse.from(tag));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(@RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID id) {
        tagUseCase.deleteTag(TagId.of(id), TenantId.of(tenantId));
        return ResponseEntity.noContent().build();
    }
}
