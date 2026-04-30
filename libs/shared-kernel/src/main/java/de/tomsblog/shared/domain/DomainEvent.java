package de.tomsblog.shared.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker interface for domain events. All events must carry metadata.
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();

    String eventType();
}
