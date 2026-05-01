package de.tomsblog.blogcontent.adapter.inbound.rest;

import de.tomsblog.blogcontent.application.port.inbound.CreateTranslationCommand;
import de.tomsblog.blogcontent.application.port.inbound.TranslationUseCase;
import de.tomsblog.blogcontent.application.port.inbound.UpdateTranslationCommand;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.Translation;
import de.tomsblog.blogcontent.domain.model.TranslationId;
import de.tomsblog.shared.tenant.TenantId;
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
@SuppressWarnings("null")
public class TranslationController {

    private final TranslationUseCase translationUseCase;

    public TranslationController(TranslationUseCase translationUseCase) {
        this.translationUseCase = translationUseCase;
    }

    @PostMapping
    public ResponseEntity<TranslationResponse> createTranslation(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
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
    public ResponseEntity<TranslationResponse> getTranslation(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID postId, @PathVariable UUID translationId) {
        Translation translation =
                translationUseCase.getTranslation(TranslationId.of(translationId), TenantId.of(tenantId));
        return ResponseEntity.ok(TranslationResponse.from(translation));
    }

    @GetMapping
    public ResponseEntity<List<TranslationResponse>> listTranslations(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID postId) {
        List<Translation> translations = translationUseCase.listTranslations(PostId.of(postId), TenantId.of(tenantId));
        List<TranslationResponse> response =
                translations.stream().map(TranslationResponse::from).toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{translationId}")
    public ResponseEntity<TranslationResponse> updateTranslation(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID postId,
            @PathVariable UUID translationId,
            @Valid @RequestBody UpdateTranslationRequest request) {
        UpdateTranslationCommand command = new UpdateTranslationCommand(
                TranslationId.of(translationId), TenantId.of(tenantId), request.title(), request.content());
        Translation translation = translationUseCase.updateTranslation(command);
        return ResponseEntity.ok(TranslationResponse.from(translation));
    }

    @PostMapping("/{translationId}/approve")
    public ResponseEntity<Void> approveTranslation(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID postId, @PathVariable UUID translationId) {
        translationUseCase.approveTranslation(TranslationId.of(translationId), TenantId.of(tenantId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{translationId}/reject")
    public ResponseEntity<Void> rejectTranslation(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID postId, @PathVariable UUID translationId) {
        translationUseCase.rejectTranslation(TranslationId.of(translationId), TenantId.of(tenantId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{translationId}")
    public ResponseEntity<Void> deleteTranslation(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID postId, @PathVariable UUID translationId) {
        translationUseCase.deleteTranslation(TranslationId.of(translationId), TenantId.of(tenantId));
        return ResponseEntity.noContent().build();
    }
}
