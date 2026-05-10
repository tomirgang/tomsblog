package de.tomsblog.blogcontent.adapter.outbound.kafka;

import de.tomsblog.blogcontent.domain.event.PostCreatedEvent;
import de.tomsblog.blogcontent.domain.event.PostPublishedEvent;
import de.tomsblog.blogcontent.domain.event.PostUpdatedEvent;
import de.tomsblog.events.EventMetadata;
import de.tomsblog.shared.domain.DomainEvent;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps internal domain events to external contract events for Kafka publication.
 * Events without an external contract (e.g. Translation events) are skipped.
 *
 * @req SWR-009
 */
final class DomainEventMapper {

    private static final Logger log = LoggerFactory.getLogger(DomainEventMapper.class);

    private DomainEventMapper() {}

    record MappedEvent(Object contractEvent, String tenantKey) {}

    static Optional<MappedEvent> toMappedEvent(DomainEvent domainEvent) {
        return switch (domainEvent) {
            case PostCreatedEvent e -> {
                String tenantKey = e.tenantId().toString();
                yield Optional.of(new MappedEvent(
                        new de.tomsblog.events.PostCreatedEvent(
                                EventMetadata.withCorrelation(
                                        e.tenantId(), e.eventId().toString(), "PostService"),
                                e.postId().value(),
                                e.title(),
                                e.slug(),
                                e.locale()),
                        tenantKey));
            }
            case PostUpdatedEvent e -> {
                String tenantKey = e.tenantId().toString();
                yield Optional.of(new MappedEvent(
                        new de.tomsblog.events.PostUpdatedEvent(
                                EventMetadata.withCorrelation(
                                        e.tenantId(), e.eventId().toString(), "PostService"),
                                e.postId().value(),
                                e.title(),
                                e.slug(),
                                e.locale()),
                        tenantKey));
            }
            case PostPublishedEvent e -> {
                String tenantKey = e.tenantId().toString();
                yield Optional.of(new MappedEvent(
                        new de.tomsblog.events.PostPublishedEvent(
                                EventMetadata.withCorrelation(
                                        e.tenantId(), e.eventId().toString(), "PostService"),
                                e.postId().value(),
                                e.slug(),
                                e.locale(),
                                e.publishedAt()),
                        tenantKey));
            }
            default -> {
                log.debug("Skipping local-only domain event: {}", domainEvent.eventType());
                yield Optional.empty();
            }
        };
    }
}
