package de.tomsblog.blogcontent.domain.model;

import de.tomsblog.shared.domain.EntityId;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed translation identifier.
 */
public record TranslationId(UUID value) implements EntityId {

    public TranslationId {
        Objects.requireNonNull(value, "TranslationId must not be null");
    }

    public static TranslationId generate() {
        return new TranslationId(UUID.randomUUID());
    }

    public static TranslationId of(UUID value) {
        return new TranslationId(value);
    }
}
