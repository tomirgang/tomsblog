package de.tomsblog.events;

import de.tomsblog.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Published when a blog post is updated (content, title, tags, etc.).
 */
public record PostUpdatedEvent(EventMetadata metadata, UUID postId, String title, String slug, String locale)
        implements DomainEvent {

    @Override
    public UUID eventId() {
        return metadata.eventId();
    }

    @Override
    public Instant occurredAt() {
        return metadata.occurredAt();
    }

    @Override
    public String eventType() {
        return "post.updated";
    }
}
