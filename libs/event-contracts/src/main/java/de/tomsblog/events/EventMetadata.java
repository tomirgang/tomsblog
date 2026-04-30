package de.tomsblog.events;

import de.tomsblog.shared.domain.DomainEvent;
import de.tomsblog.shared.tenant.TenantId;

import java.time.Instant;
import java.util.UUID;

/**
 * Base record for all events that include tenant context and correlation
 * tracking.
 */
public record EventMetadata(
        UUID eventId,
        Instant occurredAt,
        TenantId tenantId,
        String correlationId,
        String causedBy) {

    public static EventMetadata now(TenantId tenantId, String causedBy) {
        return new EventMetadata(
                UUID.randomUUID(),
                Instant.now(),
                tenantId,
                UUID.randomUUID().toString(),
                causedBy);
    }

    public static EventMetadata withCorrelation(TenantId tenantId, String correlationId, String causedBy) {
        return new EventMetadata(
                UUID.randomUUID(),
                Instant.now(),
                tenantId,
                correlationId,
                causedBy);
    }
}
