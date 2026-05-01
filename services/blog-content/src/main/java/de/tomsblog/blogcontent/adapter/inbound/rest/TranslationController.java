package de.tomsblog.blogcontent.adapter.inbound.rest;

import de.tomsblog.blogcontent.application.port.inbound.CreateTranslationCommand;
import de.tomsblog.blogcontent.application.port.inbound.TranslationUseCase;
import de.tomsblog.blogcontent.application.port.inbound.UpdateTranslationCommand;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.Translation;
import de.tomsblog.blogcontent.domain.model.TranslationId;
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
 * REST adapter for translation management.
 *
 * @req SWR-021
 * @req SWR-015
 */
@RestController
@RequestMapping("/api/posts/{postId}/translations")
@Tag(name = "Translations", description = "Multilingual translation management for posts")
@SuppressWarnings("null")
public class TranslationController {

    private final TranslationUseCase translationUseCase;

    public TranslationController(TranslationUseCase translationUseCase) {
        this.translationUseCase = translationUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Create a translation",
            description = "Creates a manual or AI-generated translation for a post.")
    @ApiResponse(responseCode = "201", description = "Translation created")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    public ResponseEntity<TranslationResponse> createTranslation(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID postId,
            @Valid @RequestBody CreateTranslationRequest request) {
        CreateTranslationCommand command = new CreateTranslationCommand(
                PostId.of(postId), TenantId.of(tenantId), request.locale(), request.title(), request.content());
        Translation translation;
        if ("AI_GENERATED".equals(request.source())) {
            translation = translationUseCase.createAiTranslation(command);
        } else {
            translation = translationUseCase.createManualTranslation(command);
        }
        return ResponseEntity.created(URI.create("/api/posts/" + postId + "/translations/"
                        + translation.getId().value()))
                .body(TranslationResponse.from(translation));
    }

    @GetMapping("/{translationId}")
    @Operation(summary = "Get a translation by ID")
    @ApiResponse(responseCode = "200", description = "Translation found")
    @ApiResponse(responseCode = "404", description = "Translation not found")
    public ResponseEntity<TranslationResponse> getTranslation(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID postId,
            @PathVariable UUID translationId) {
        Translation translation =
                translationUseCase.getTranslation(TranslationId.of(translationId), TenantId.of(tenantId));
        return ResponseEntity.ok(TranslationResponse.from(translation));
    }

    @GetMapping
    @Operation(summary = "List all translations for a post")
    @ApiResponse(responseCode = "200", description = "List of translations")
    public ResponseEntity<List<TranslationResponse>> listTranslations(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID postId) {
        List<Translation> translations = translationUseCase.listTranslations(PostId.of(postId), TenantId.of(tenantId));
        List<TranslationResponse> response =
                translations.stream().map(TranslationResponse::from).toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{translationId}")
    @Operation(summary = "Update a translation", description = "Updates the title and content of a translation.")
    @ApiResponse(responseCode = "200", description = "Translation updated")
    @ApiResponse(responseCode = "404", description = "Translation not found")
    public ResponseEntity<TranslationResponse> updateTranslation(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID postId,
            @PathVariable UUID translationId,
            @Valid @RequestBody UpdateTranslationRequest request) {
        UpdateTranslationCommand command = new UpdateTranslationCommand(
                TranslationId.of(translationId), TenantId.of(tenantId), request.title(), request.content());
        Translation translation = translationUseCase.updateTranslation(command);
        return ResponseEntity.ok(TranslationResponse.from(translation));
    }

    @PostMapping("/{translationId}/approve")
    @Operation(summary = "Approve a translation", description = "Approves an AI-generated translation after review.")
    @ApiResponse(responseCode = "204", description = "Translation approved")
    @ApiResponse(responseCode = "404", description = "Translation not found")
    @ApiResponse(responseCode = "409", description = "Translation is not in REVIEW_PENDING status")
    public ResponseEntity<Void> approveTranslation(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID postId,
            @PathVariable UUID translationId) {
        translationUseCase.approveTranslation(TranslationId.of(translationId), TenantId.of(tenantId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{translationId}/reject")
    @Operation(summary = "Reject a translation", description = "Rejects an AI-generated translation after review.")
    @ApiResponse(responseCode = "204", description = "Translation rejected")
    @ApiResponse(responseCode = "404", description = "Translation not found")
    @ApiResponse(responseCode = "409", description = "Translation is not in REVIEW_PENDING status")
    public ResponseEntity<Void> rejectTranslation(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID postId,
            @PathVariable UUID translationId) {
        translationUseCase.rejectTranslation(TranslationId.of(translationId), TenantId.of(tenantId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{translationId}")
    @Operation(summary = "Delete a translation")
    @ApiResponse(responseCode = "204", description = "Translation deleted")
    public ResponseEntity<Void> deleteTranslation(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID postId,
            @PathVariable UUID translationId) {
        translationUseCase.deleteTranslation(TranslationId.of(translationId), TenantId.of(tenantId));
        return ResponseEntity.noContent().build();
    }
}
