package de.tomsblog.usermanagement.adapter.outbound.persistence;

import de.tomsblog.shared.audit.AuditLogEntry;
import de.tomsblog.shared.audit.AuditLogger;
import org.springframework.stereotype.Component;

/**
 * JPA adapter persisting audit log entries to the audit_log table.
 *
 * @req SWR-056
 * @req SWA-031
 */
@Component
public class JpaAuditLogger implements AuditLogger {

    private final SpringDataAuditLogRepository repository;

    public JpaAuditLogger(SpringDataAuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void log(AuditLogEntry entry) {
        var entity = new AuditLogJpaEntity(
                entry.id(),
                entry.timestamp(),
                entry.tenantId(),
                entry.actor(),
                entry.action(),
                entry.entityType(),
                entry.entityId(),
                entry.details());
        repository.save(entity);
    }
}
