package de.tomsblog.usermanagement.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TenantSettingsJpaEntityTest {

    @Test
    @DisplayName("SWR-044: getters and setters work correctly")
    void gettersAndSetters() {
        var entity = new TenantSettingsJpaEntity();
        var id = UUID.randomUUID();

        entity.setTenantId(id);
        entity.setLoginMode("BOTH");
        entity.setAutoApproveOidc(true);
        entity.setAutoApproveEmailDomains(Set.of("test.com"));

        assertThat(entity.getTenantId()).isEqualTo(id);
        assertThat(entity.getLoginMode()).isEqualTo("BOTH");
        assertThat(entity.isAutoApproveOidc()).isTrue();
        assertThat(entity.getAutoApproveEmailDomains()).containsExactly("test.com");
    }

    @Test
    @DisplayName("SWR-044: updatedAt set on prePersist")
    void updatedAtSetOnPrePersist() {
        var entity = new TenantSettingsJpaEntity();
        assertThat(entity.getUpdatedAt()).isNull();

        entity.prePersist();

        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("SWR-044: updatedAt updated on preUpdate")
    void updatedAtUpdatedOnPreUpdate() {
        var entity = new TenantSettingsJpaEntity();
        entity.prePersist();
        var first = entity.getUpdatedAt();

        entity.preUpdate();

        assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(first);
    }
}
