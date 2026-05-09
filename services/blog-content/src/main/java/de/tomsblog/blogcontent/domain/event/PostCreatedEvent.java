package de.tomsblog.blogcontent.domain.event;

import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.shared.domain.DomainEvent;
import de.tomsblog.shared.tenant.TenantId;
import java.time.Instant;
import java.util.UUID;

/** @req SWR-009 */
public record PostCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        PostId postId,
        TenantId tenantId,
        String title,
        String slug,
        String locale)
        implements DomainEvent {

    public static PostCreatedEvent of(PostId postId, TenantId tenantId, String title, String slug, String locale) {
        return new PostCreatedEvent(
                UUID.randomUUID(), Instant.now(), "post.created", postId, tenantId, title, slug, locale);
    }
}
