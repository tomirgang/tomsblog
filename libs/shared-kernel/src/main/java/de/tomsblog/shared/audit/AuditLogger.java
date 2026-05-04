package de.tomsblog.shared.audit;

/**
 * Outbound port for persisting audit log entries.
 * Each service provides its own adapter implementation.
 *
 * @req SWR-056
 * @req SWA-031
 */
public interface AuditLogger {

    void log(AuditLogEntry entry);
}
