package de.tomsblog.tenantmanagement.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(JpaAuditLogger.class)
class JpaAuditLoggerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("tenant_management_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private JpaAuditLogger auditLogger;

    @Autowired
    private SpringDataAuditLogRepository auditLogRepository;

    @Test
    @DisplayName("SWR-056: audit log entry is persisted")
    void logPersistsEntry() {
        var entry = AuditLogEntry.create("tenant-1", "admin", "TENANT_CREATED", "Tenant", "tenant-1", "slug=blog");

        auditLogger.log(entry);

        var all = auditLogRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.getFirst().getAction()).isEqualTo("TENANT_CREATED");
        assertThat(all.getFirst().getDetails()).isEqualTo("slug=blog");
    }
}
