package de.tomsblog.blogcontent.domain.event;

import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.PostLocale;
import de.tomsblog.blogcontent.domain.model.TranslationId;
import de.tomsblog.shared.domain.DomainEvent;
import de.tomsblog.shared.tenant.TenantId;
import java.time.Instant;
import java.util.UUID;

/** @req SWR-009 */
public record TranslationApprovedEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        TranslationId translationId,
        PostId postId,
        TenantId tenantId,
        PostLocale locale)
        implements DomainEvent {

    public static TranslationApprovedEvent of(
            TranslationId translationId, PostId postId, TenantId tenantId, PostLocale locale) {
        return new TranslationApprovedEvent(
                UUID.randomUUID(), Instant.now(), "translation.approved", translationId, postId, tenantId, locale);
    }
}
