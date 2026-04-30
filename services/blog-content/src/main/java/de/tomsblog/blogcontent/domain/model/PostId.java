package de.tomsblog.blogcontent.domain.model;

import de.tomsblog.shared.domain.EntityId;
import java.util.Objects;
import java.util.UUID;

public record PostId(UUID value) implements EntityId {

    public PostId {
        Objects.requireNonNull(value, "PostId must not be null");
    }

    public static PostId generate() {
        return new PostId(UUID.randomUUID());
    }

    public static PostId of(UUID value) {
        return new PostId(value);
    }
}
