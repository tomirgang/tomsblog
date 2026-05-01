package de.tomsblog.blogcontent.adapter.inbound.rest;

import de.tomsblog.blogcontent.application.port.inbound.CreateTagCommand;
import de.tomsblog.blogcontent.application.port.inbound.RenameTagCommand;
import de.tomsblog.blogcontent.application.port.inbound.TagUseCase;
import de.tomsblog.blogcontent.domain.model.Tag;
import de.tomsblog.blogcontent.domain.model.TagId;
import de.tomsblog.shared.tenant.TenantId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST adapter for tag management.
 *
 * @req SWR-020
 * @req SWR-015
 */
@RestController
@RequestMapping("/api/tags")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags", description = "Tag CRUD operations for categorizing posts")
@SuppressWarnings("null")
public class TagController {

    private final TagUseCase tagUseCase;

    public TagController(TagUseCase tagUseCase) {
        this.tagUseCase = tagUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new tag", description = "Creates a tag with a unique name per tenant.")
    @ApiResponse(responseCode = "201", description = "Tag created")
    @ApiResponse(responseCode = "400", description = "Invalid request or duplicate tag name")
    public ResponseEntity<TagResponse> createTag(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody CreateTagRequest request) {
        CreateTagCommand command = new CreateTagCommand(TenantId.of(tenantId), request.name());
        Tag tag = tagUseCase.createTag(command);
        return ResponseEntity.created(URI.create("/api/tags/" + tag.getId().value()))
                .body(TagResponse.from(tag));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a tag by ID")
    @ApiResponse(responseCode = "200", description = "Tag found")
    @ApiResponse(responseCode = "404", description = "Tag not found")
    public ResponseEntity<TagResponse> getTag(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID id) {
        Tag tag = tagUseCase.getTag(TagId.of(id), TenantId.of(tenantId));
        return ResponseEntity.ok(TagResponse.from(tag));
    }

    @GetMapping
    @Operation(summary = "List all tags for a tenant")
    @ApiResponse(responseCode = "200", description = "List of tags")
    public ResponseEntity<List<TagResponse>> listTags(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId) {
        List<Tag> tags = tagUseCase.listTags(TenantId.of(tenantId));
        List<TagResponse> response = tags.stream().map(TagResponse::from).toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Rename a tag")
    @ApiResponse(responseCode = "200", description = "Tag renamed")
    @ApiResponse(responseCode = "404", description = "Tag not found")
    public ResponseEntity<TagResponse> renameTag(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody RenameTagRequest request) {
        RenameTagCommand command = new RenameTagCommand(TagId.of(id), TenantId.of(tenantId), request.name());
        Tag tag = tagUseCase.renameTag(command);
        return ResponseEntity.ok(TagResponse.from(tag));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a tag")
    @ApiResponse(responseCode = "204", description = "Tag deleted")
    @ApiResponse(responseCode = "404", description = "Tag not found")
    public ResponseEntity<Void> deleteTag(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID id) {
        tagUseCase.deleteTag(TagId.of(id), TenantId.of(tenantId));
        return ResponseEntity.noContent().build();
    }
}
