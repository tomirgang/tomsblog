package de.tomsblog.usermanagement.domain.model;

import de.tomsblog.shared.domain.EntityId;
import java.util.UUID;

/**
 * Strongly-typed identifier for user profiles.
 *
 * @req SWR-043
 */
public record UserProfileId(UUID value) implements EntityId {

    public UserProfileId {
        if (value == null) {
            throw new IllegalArgumentException("UserProfileId must not be null");
        }
    }

    public static UserProfileId generate() {
        return new UserProfileId(UUID.randomUUID());
    }

    public static UserProfileId of(UUID value) {
        return new UserProfileId(value);
    }
}
