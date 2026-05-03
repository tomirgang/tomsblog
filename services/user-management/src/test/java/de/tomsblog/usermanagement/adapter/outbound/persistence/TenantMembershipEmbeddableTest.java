package de.tomsblog.usermanagement.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TenantMembershipEmbeddableTest {

    @Test
    @DisplayName("SWR-043: equals returns true for same instance")
    void equalsSameInstance() {
        var embeddable = new TenantMembershipEmbeddable(UUID.randomUUID(), RoleJpa.AUTHOR);

        assertThat(embeddable).isEqualTo(embeddable);
    }

    @Test
    @DisplayName("SWR-043: equals returns true for equal values")
    void equalsEqualValues() {
        UUID tenantId = UUID.randomUUID();
        var a = new TenantMembershipEmbeddable(tenantId, RoleJpa.AUTHOR);
        var b = new TenantMembershipEmbeddable(tenantId, RoleJpa.AUTHOR);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("SWR-043: equals returns false for different tenantId")
    void equalsDifferentTenantId() {
        var a = new TenantMembershipEmbeddable(UUID.randomUUID(), RoleJpa.AUTHOR);
        var b = new TenantMembershipEmbeddable(UUID.randomUUID(), RoleJpa.AUTHOR);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("SWR-043: equals returns false for different role")
    void equalsDifferentRole() {
        UUID tenantId = UUID.randomUUID();
        var a = new TenantMembershipEmbeddable(tenantId, RoleJpa.AUTHOR);
        var b = new TenantMembershipEmbeddable(tenantId, RoleJpa.ADMIN);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("SWR-043: equals returns false for null")
    void equalsNull() {
        var embeddable = new TenantMembershipEmbeddable(UUID.randomUUID(), RoleJpa.AUTHOR);

        assertThat(embeddable).isNotEqualTo(null);
    }

    @Test
    @DisplayName("SWR-043: equals returns false for different type")
    void equalsDifferentType() {
        var embeddable = new TenantMembershipEmbeddable(UUID.randomUUID(), RoleJpa.AUTHOR);

        assertThat(embeddable).isNotEqualTo("not an embeddable");
    }

    @Test
    @DisplayName("SWR-043: setters update fields")
    void settersUpdateFields() {
        var embeddable = new TenantMembershipEmbeddable();
        UUID tenantId = UUID.randomUUID();

        embeddable.setTenantId(tenantId);
        embeddable.setRole(RoleJpa.REVIEWER);

        assertThat(embeddable.getTenantId()).isEqualTo(tenantId);
        assertThat(embeddable.getRole()).isEqualTo(RoleJpa.REVIEWER);
    }
}
