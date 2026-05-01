package de.tomsblog.blogcontent.adapter.inbound.rest;

import de.tomsblog.blogcontent.domain.model.Translation;
import java.time.Instant;
import java.util.UUID;

public record TranslationResponse(
        UUID id,
        UUID postId,
        UUID tenantId,
        String locale,
        String title,
        String content,
        String status,
        String source,
        Instant translatedAt) {

    public static TranslationResponse from(Translation translation) {
        return new TranslationResponse(
                translation.getId().value(),
                translation.getPostId().value(),
                translation.getTenantId().value(),
                translation.getLocale().languageTag(),
                translation.getTitle(),
                translation.getContent(),
                translation.getStatus().name(),
                translation.getSource().name(),
                translation.getTranslatedAt());
    }
}
