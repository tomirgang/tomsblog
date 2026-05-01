package de.tomsblog.events;

import de.tomsblog.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Published when a blog post is created.
 */
public record PostCreatedEvent(EventMetadata metadata, UUID postId, String title, String slug, String locale)
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
        return "post.created";
    }
}
