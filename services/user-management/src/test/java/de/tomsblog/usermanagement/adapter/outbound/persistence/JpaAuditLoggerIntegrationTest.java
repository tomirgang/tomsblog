package de.tomsblog.usermanagement.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.shared.audit.AuditLogEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @req SWR-056
 * @req SWA-031
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditLogger.class)
class JpaAuditLoggerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private JpaAuditLogger auditLogger;

    @Autowired
    private SpringDataAuditLogRepository repository;

    @Test
    @DisplayName("SWR-056: log persists audit entry to database")
    void logPersistsEntry() {
        AuditLogEntry entry = AuditLogEntry.create(
                "tenant-1", "admin", "USER_APPROVED", "UserProfile", "user-123", "approved via admin UI");

        auditLogger.log(entry);

        var saved = repository.findById(entry.id());
        assertThat(saved).isPresent();
        assertThat(saved.get().getTenantId()).isEqualTo("tenant-1");
        assertThat(saved.get().getActor()).isEqualTo("admin");
        assertThat(saved.get().getAction()).isEqualTo("USER_APPROVED");
        assertThat(saved.get().getEntityType()).isEqualTo("UserProfile");
        assertThat(saved.get().getEntityId()).isEqualTo("user-123");
        assertThat(saved.get().getDetails()).isEqualTo("approved via admin UI");
        assertThat(saved.get().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("SWR-056: log persists entry without details")
    void logPersistsEntryWithoutDetails() {
        AuditLogEntry entry = AuditLogEntry.create("tenant-1", "system", "USER_REJECTED", "UserProfile", "user-456");

        auditLogger.log(entry);

        var saved = repository.findById(entry.id());
        assertThat(saved).isPresent();
        assertThat(saved.get().getDetails()).isNull();
    }

    @Test
    @DisplayName("SWR-056: log persists entry with null tenantId")
    void logPersistsEntryWithNullTenantId() {
        AuditLogEntry entry =
                AuditLogEntry.create(null, "system", "GLOBAL_ROLE_ASSIGNED", "UserProfile", "user-789", "ADMIN");

        auditLogger.log(entry);

        var saved = repository.findById(entry.id());
        assertThat(saved).isPresent();
        assertThat(saved.get().getTenantId()).isNull();
    }
}
