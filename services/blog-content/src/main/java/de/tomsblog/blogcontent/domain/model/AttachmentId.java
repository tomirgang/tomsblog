package de.tomsblog.blogcontent.domain.model;

import de.tomsblog.shared.domain.EntityId;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed attachment identifier.
 */
public record AttachmentId(UUID value) implements EntityId {

    public AttachmentId {
        Objects.requireNonNull(value, "AttachmentId must not be null");
    }

    public static AttachmentId generate() {
        return new AttachmentId(UUID.randomUUID());
    }

    public static AttachmentId of(UUID value) {
        return new AttachmentId(value);
    }
}
