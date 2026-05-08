package de.tomsblog.blogcontent.application.port.inbound;

import java.time.Instant;

/**
 * Criteria for filtering audit log entries.
 * All fields are optional; {@code null} means no filter for that field.
 *
 * @req SWR-087
 */
public record AuditLogSearchCriteria(
        String action, String entityType, String actor, Instant from, Instant to, String searchTerm) {

    public boolean hasFilters() {
        return action != null
                || entityType != null
                || actor != null
                || from != null
                || to != null
                || searchTerm != null;
    }
}
