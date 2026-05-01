package de.tomsblog.shared.domain;

import java.util.UUID;

/**
 * Base class for strongly-typed entity identifiers.
 * Subclass as a record:
 * {@code public record PostId(UUID value) implements EntityId}
 */
public interface EntityId {

    UUID value();

    default String asString() {
        return value().toString();
    }

    static UUID generate() {
        return UUID.randomUUID();
    }
}
