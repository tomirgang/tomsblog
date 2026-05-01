package de.tomsblog.events;

import de.tomsblog.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Published when a blog post transitions to "published" state.
 */
public record PostPublishedEvent(EventMetadata metadata, UUID postId, String slug, String locale, Instant publishedAt)
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
        return "post.published";
    }
}
