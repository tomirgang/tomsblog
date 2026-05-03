package de.tomsblog.usermanagement.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserProfileJpaEntityTest {

    @Test
    @DisplayName("SWR-043: updatedAt getters and setters work")
    void updatedAtGetterSetter() {
        var entity = new UserProfileJpaEntity();
        Instant now = Instant.now();

        entity.setUpdatedAt(now);

        assertThat(entity.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("SWR-043: default constructor initializes empty collections")
    void defaultConstructorInitializesCollections() {
        var entity = new UserProfileJpaEntity();

        assertThat(entity.getGlobalRoles()).isNotNull().isEmpty();
        assertThat(entity.getTenantMemberships()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("SWR-043: prePersist sets updatedAt")
    void prePersistSetsUpdatedAt() {
        var entity = new UserProfileJpaEntity();
        entity.setId(UUID.randomUUID());

        entity.prePersist();

        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("SWR-043: preUpdate sets updatedAt")
    void preUpdateSetsUpdatedAt() {
        var entity = new UserProfileJpaEntity();

        entity.preUpdate();

        assertThat(entity.getUpdatedAt()).isNotNull();
    }
}
