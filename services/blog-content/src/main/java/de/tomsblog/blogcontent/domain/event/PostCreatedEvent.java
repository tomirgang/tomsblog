package de.tomsblog.blogcontent.domain.event;

import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.shared.domain.DomainEvent;
import de.tomsblog.shared.tenant.TenantId;
import java.time.Instant;
import java.util.UUID;

/** @req SWR-009 */
public record PostCreatedEvent(UUID eventId, Instant occurredAt, String eventType, PostId postId, TenantId tenantId)
        implements DomainEvent {

    public static PostCreatedEvent of(PostId postId, TenantId tenantId) {
        return new PostCreatedEvent(UUID.randomUUID(), Instant.now(), "post.created", postId, tenantId);
    }
}
