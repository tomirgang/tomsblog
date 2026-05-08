package de.tomsblog.tenantmanagement.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only JPA entity for the audit_log table.
 *
 * @req SWR-056
 */
@Entity
@Table(name = "audit_log")
public class AuditLogJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "tenant_id", updatable = false)
    private String tenantId;

    @Column(name = "actor", nullable = false, updatable = false)
    private String actor;

    @Column(name = "action", nullable = false, updatable = false)
    private String action;

    @Column(name = "entity_type", nullable = false, updatable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false, updatable = false)
    private String entityId;

    @Column(name = "details", updatable = false)
    private String details;

    protected AuditLogJpaEntity() {}

    public AuditLogJpaEntity(
            UUID id,
            Instant timestamp,
            String tenantId,
            String actor,
            String action,
            String entityType,
            String entityId,
            String details) {
        this.id = id;
        this.timestamp = timestamp;
        this.tenantId = tenantId;
        this.actor = actor;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
    }

    public UUID getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDetails() {
        return details;
    }
}
