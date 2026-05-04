package de.tomsblog.blogcontent.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.shared.audit.AuditLogEntry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @req SWR-056
 */
class AuditLogEntryTest {

    @Test
    @DisplayName("SWR-056: create generates entry with all fields")
    void createWithDetails() {
        AuditLogEntry entry =
                AuditLogEntry.create("tenant-1", "admin", "POST_CREATED", "Post", "post-123", "some details");

        assertThat(entry.id()).isNotNull();
        assertThat(entry.timestamp()).isNotNull();
        assertThat(entry.tenantId()).isEqualTo("tenant-1");
        assertThat(entry.actor()).isEqualTo("admin");
        assertThat(entry.action()).isEqualTo("POST_CREATED");
        assertThat(entry.entityType()).isEqualTo("Post");
        assertThat(entry.entityId()).isEqualTo("post-123");
        assertThat(entry.details()).isEqualTo("some details");
    }

    @Test
    @DisplayName("SWR-056: create without details sets details to null")
    void createWithoutDetails() {
        AuditLogEntry entry = AuditLogEntry.create("tenant-1", "admin", "POST_DELETED", "Post", "post-123");

        assertThat(entry.details()).isNull();
    }

    @Test
    @DisplayName("SWR-056: create with null tenantId is allowed")
    void createWithNullTenantId() {
        AuditLogEntry entry = AuditLogEntry.create(null, "admin", "USER_APPROVED", "UserProfile", "user-1");

        assertThat(entry.tenantId()).isNull();
    }

    @Test
    @DisplayName("SWR-056: constructor rejects null required fields")
    void constructorRejectsNullRequiredFields() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        assertThatThrownBy(() -> new AuditLogEntry(null, now, "t", "a", "act", "et", "eid", null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuditLogEntry(id, null, "t", "a", "act", "et", "eid", null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuditLogEntry(id, now, "t", null, "act", "et", "eid", null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuditLogEntry(id, now, "t", "a", null, "et", "eid", null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuditLogEntry(id, now, "t", "a", "act", null, "eid", null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuditLogEntry(id, now, "t", "a", "act", "et", null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
