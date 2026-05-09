package de.tomsblog.blogcontent.adapter.outbound.kafka;

import de.tomsblog.blogcontent.domain.event.PostCreatedEvent;
import de.tomsblog.blogcontent.domain.event.PostPublishedEvent;
import de.tomsblog.blogcontent.domain.event.PostUpdatedEvent;
import de.tomsblog.events.EventMetadata;
import de.tomsblog.shared.domain.DomainEvent;

/**
 * Maps internal domain events to external contract events for Kafka publication.
 *
 * @req SWR-009
 */
final class DomainEventMapper {

    private DomainEventMapper() {}

    static Object toContractEvent(DomainEvent domainEvent) {
        return switch (domainEvent) {
            case PostCreatedEvent e ->
                new de.tomsblog.events.PostCreatedEvent(
                        EventMetadata.withCorrelation(e.tenantId(), e.eventId().toString(), "PostService"),
                        e.postId().value(),
                        e.title(),
                        e.slug(),
                        e.locale());
            case PostUpdatedEvent e ->
                new de.tomsblog.events.PostUpdatedEvent(
                        EventMetadata.withCorrelation(e.tenantId(), e.eventId().toString(), "PostService"),
                        e.postId().value(),
                        e.title(),
                        e.slug(),
                        e.locale());
            case PostPublishedEvent e ->
                new de.tomsblog.events.PostPublishedEvent(
                        EventMetadata.withCorrelation(e.tenantId(), e.eventId().toString(), "PostService"),
                        e.postId().value(),
                        e.slug(),
                        e.locale(),
                        e.publishedAt());
            default -> throw new IllegalArgumentException("Unknown domain event type: " + domainEvent.getClass());
        };
    }
}
