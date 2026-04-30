package de.tomsblog.shared.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed author identifier. Shared across services.
 */
public record AuthorId(UUID value) implements EntityId {

    public AuthorId {
        Objects.requireNonNull(value, "AuthorId must not be null");
    }

    public static AuthorId generate() {
        return new AuthorId(UUID.randomUUID());
    }

    public static AuthorId of(UUID value) {
        return new AuthorId(value);
    }
}
