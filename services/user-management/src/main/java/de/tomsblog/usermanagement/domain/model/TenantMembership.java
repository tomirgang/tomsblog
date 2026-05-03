package de.tomsblog.usermanagement.domain.model;

import de.tomsblog.shared.tenant.TenantId;
import java.util.Objects;

/**
 * Value object representing a user's membership in a tenant with a specific role.
 *
 * @req SWR-043
 */
public record TenantMembership(TenantId tenantId, Role role) {

    public TenantMembership {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(role, "role must not be null");
    }
}
