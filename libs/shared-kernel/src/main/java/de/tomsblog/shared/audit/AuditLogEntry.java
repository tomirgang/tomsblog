package de.tomsblog.shared.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit log entry capturing who did what and when.
 * Framework-free record for use across all services.
 *
 * @req SWR-056
 */
public record AuditLogEntry(
        UUID id,
        Instant timestamp,
        String tenantId,
        String actor,
        String action,
        String entityType,
        String entityId,
        String details) {

    public AuditLogEntry {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(entityType, "entityType must not be null");
        Objects.requireNonNull(entityId, "entityId must not be null");
    }

    public static AuditLogEntry create(
            String tenantId, String actor, String action, String entityType, String entityId, String details) {
        return new AuditLogEntry(
                UUID.randomUUID(), Instant.now(), tenantId, actor, action, entityType, entityId, details);
    }

    public static AuditLogEntry create(
            String tenantId, String actor, String action, String entityType, String entityId) {
        return create(tenantId, actor, action, entityType, entityId, null);
    }
}
