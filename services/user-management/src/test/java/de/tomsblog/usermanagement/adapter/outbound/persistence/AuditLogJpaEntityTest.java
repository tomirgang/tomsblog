package de.tomsblog.usermanagement.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @req SWR-056
 */
class AuditLogJpaEntityTest {

    @Test
    @DisplayName("SWR-056: constructor sets all fields")
    void constructorSetsAllFields() {
        UUID id = UUID.randomUUID();
        Instant timestamp = Instant.now();

        var entity = new AuditLogJpaEntity(
                id, timestamp, "tenant-1", "admin", "USER_APPROVED", "UserProfile", "user-123", "details");

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getTimestamp()).isEqualTo(timestamp);
        assertThat(entity.getTenantId()).isEqualTo("tenant-1");
        assertThat(entity.getActor()).isEqualTo("admin");
        assertThat(entity.getAction()).isEqualTo("USER_APPROVED");
        assertThat(entity.getEntityType()).isEqualTo("UserProfile");
        assertThat(entity.getEntityId()).isEqualTo("user-123");
        assertThat(entity.getDetails()).isEqualTo("details");
    }

    @Test
    @DisplayName("SWR-056: nullable fields can be null")
    void nullableFieldsCanBeNull() {
        var entity =
                new AuditLogJpaEntity(UUID.randomUUID(), Instant.now(), null, "system", "ACTION", "Type", "id-1", null);

        assertThat(entity.getTenantId()).isNull();
        assertThat(entity.getDetails()).isNull();
    }
}
