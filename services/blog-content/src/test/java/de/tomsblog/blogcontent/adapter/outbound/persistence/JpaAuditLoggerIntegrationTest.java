package de.tomsblog.blogcontent.adapter.outbound.persistence;

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
        AuditLogEntry entry =
                AuditLogEntry.create("tenant-1", "admin", "POST_CREATED", "Post", "post-123", "title=Test Post");

        auditLogger.log(entry);

        var saved = repository.findById(entry.id());
        assertThat(saved).isPresent();
        assertThat(saved.get().getTenantId()).isEqualTo("tenant-1");
        assertThat(saved.get().getActor()).isEqualTo("admin");
        assertThat(saved.get().getAction()).isEqualTo("POST_CREATED");
        assertThat(saved.get().getEntityType()).isEqualTo("Post");
        assertThat(saved.get().getEntityId()).isEqualTo("post-123");
        assertThat(saved.get().getDetails()).isEqualTo("title=Test Post");
        assertThat(saved.get().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("SWR-056: log persists entry without details")
    void logPersistsEntryWithoutDetails() {
        AuditLogEntry entry = AuditLogEntry.create("tenant-1", "system", "TAG_DELETED", "Tag", "tag-456");

        auditLogger.log(entry);

        var saved = repository.findById(entry.id());
        assertThat(saved).isPresent();
        assertThat(saved.get().getDetails()).isNull();
    }
}
