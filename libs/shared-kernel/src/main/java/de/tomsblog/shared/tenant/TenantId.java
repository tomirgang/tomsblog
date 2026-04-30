package de.tomsblog.shared.tenant;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed tenant identifier. Every multi-tenant entity must carry this.
 */
public record TenantId(UUID value) {

    public TenantId {
        Objects.requireNonNull(value, "TenantId must not be null");
    }

    public static TenantId of(UUID value) {
        return new TenantId(value);
    }

    public static TenantId generate() {
        return new TenantId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
