package de.tomsblog.tenantmanagement.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditLogJpaEntityTest {

    @Test
    @DisplayName("SWR-056: entity fields are accessible")
    void fieldsAccessible() {
        var id = UUID.randomUUID();
        var now = Instant.now();
        var entity = new AuditLogJpaEntity(id, now, "tenant-1", "admin", "CREATE", "Tenant", "t1", "details");

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getTimestamp()).isEqualTo(now);
        assertThat(entity.getTenantId()).isEqualTo("tenant-1");
        assertThat(entity.getActor()).isEqualTo("admin");
        assertThat(entity.getAction()).isEqualTo("CREATE");
        assertThat(entity.getEntityType()).isEqualTo("Tenant");
        assertThat(entity.getEntityId()).isEqualTo("t1");
        assertThat(entity.getDetails()).isEqualTo("details");
    }
}
