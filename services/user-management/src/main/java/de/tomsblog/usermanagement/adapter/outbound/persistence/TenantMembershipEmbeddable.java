package de.tomsblog.usermanagement.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA embeddable for tenant memberships.
 */
@Embeddable
public class TenantMembershipEmbeddable {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private RoleJpa role;

    protected TenantMembershipEmbeddable() {
        // JPA
    }

    public TenantMembershipEmbeddable(UUID tenantId, RoleJpa role) {
        this.tenantId = tenantId;
        this.role = role;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public RoleJpa getRole() {
        return role;
    }

    public void setRole(RoleJpa role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantMembershipEmbeddable that = (TenantMembershipEmbeddable) o;
        return Objects.equals(tenantId, that.tenantId) && role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, role);
    }
}
