package de.tomsblog.shared.audit;

import java.time.Instant;

/**
 * Audit metadata that every persistent entity should carry.
 * Adapters (JPA) are responsible for populating these fields.
 */
public interface Auditable {

    Instant getCreatedAt();

    Instant getUpdatedAt();

    String getCreatedBy();

    String getUpdatedBy();
}
