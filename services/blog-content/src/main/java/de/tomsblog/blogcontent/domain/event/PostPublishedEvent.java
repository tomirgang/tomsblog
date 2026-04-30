package de.tomsblog.blogcontent.domain.event;

import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.shared.domain.DomainEvent;
import de.tomsblog.shared.tenant.TenantId;
import java.time.Instant;
import java.util.UUID;

public record PostPublishedEvent(UUID eventId, Instant occurredAt, String eventType, PostId postId, TenantId tenantId)
        implements DomainEvent {

    public static PostPublishedEvent of(PostId postId, TenantId tenantId) {
        return new PostPublishedEvent(UUID.randomUUID(), Instant.now(), "post.published", postId, tenantId);
    }
}
