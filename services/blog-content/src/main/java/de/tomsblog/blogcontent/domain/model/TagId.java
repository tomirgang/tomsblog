package de.tomsblog.blogcontent.domain.model;

import de.tomsblog.shared.domain.EntityId;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed tag identifier.
 */
public record TagId(UUID value) implements EntityId {

    public TagId {
        Objects.requireNonNull(value, "TagId must not be null");
    }

    public static TagId generate() {
        return new TagId(UUID.randomUUID());
    }

    public static TagId of(UUID value) {
        return new TagId(value);
    }
}
