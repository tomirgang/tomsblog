package de.tomsblog.blogcontent.domain.event;

import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.PostLocale;
import de.tomsblog.blogcontent.domain.model.TranslationId;
import de.tomsblog.blogcontent.domain.model.TranslationSource;
import de.tomsblog.shared.domain.DomainEvent;
import de.tomsblog.shared.tenant.TenantId;
import java.time.Instant;
import java.util.UUID;

public record TranslationCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        TranslationId translationId,
        PostId postId,
        TenantId tenantId,
        PostLocale locale,
        TranslationSource source) implements DomainEvent {

    public static TranslationCreatedEvent of(TranslationId translationId, PostId postId,
            TenantId tenantId, PostLocale locale,
            TranslationSource source) {
        return new TranslationCreatedEvent(UUID.randomUUID(), Instant.now(), "translation.created",
                translationId, postId, tenantId, locale, source);
    }
}
