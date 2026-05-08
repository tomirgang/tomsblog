package de.tomsblog.blogcontent.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.blogcontent.application.port.inbound.AuditLogSearchCriteria;
import de.tomsblog.shared.audit.AuditLogEntry;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @req SWR-087
 * @req SWA-036
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditLogQueryAdapter.class, JpaAuditLogger.class})
class JpaAuditLogQueryAdapterIntegrationTest {

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
    private JpaAuditLogQueryAdapter adapter;

    @Autowired
    private JpaAuditLogger auditLogger;

    private static final String TENANT = "test-tenant";
    private static final String OTHER_TENANT = "other-tenant";

    @BeforeEach
    void setUp() {
        auditLogger.log(AuditLogEntry.create(TENANT, "admin", "POST_CREATED", "Post", "p1", "title=First"));
        auditLogger.log(AuditLogEntry.create(TENANT, "author1", "POST_UPDATED", "Post", "p2", "title=Second"));
        auditLogger.log(AuditLogEntry.create(TENANT, "admin", "TAG_CREATED", "Tag", "t1", "name=Java"));
        auditLogger.log(AuditLogEntry.create(TENANT, "system", "TAG_DELETED", "Tag", "t2"));
        auditLogger.log(AuditLogEntry.create(OTHER_TENANT, "other", "POST_CREATED", "Post", "p3"));
    }

    @Test
    @DisplayName("SWR-087: findByTenantId returns only entries for given tenant")
    void findByTenantId_filtersToTenant() {
        PageRequest pageable = PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<AuditLogEntry> result = adapter.findByTenantId(TENANT, pageable);

        assertThat(result.getContent()).hasSize(4);
        assertThat(result.getContent()).allMatch(e -> TENANT.equals(e.tenantId()));
    }

    @Test
    @DisplayName("SWR-087: findByTenantId supports pagination")
    void findByTenantId_supportsPagination() {
        PageRequest pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<AuditLogEntry> result = adapter.findByTenantId(TENANT, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("SWR-087: search with action filter returns matching entries")
    void search_actionFilter() {
        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria("POST_CREATED", null, null, null, null, null);
        PageRequest pageable = PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<AuditLogEntry> result = adapter.findByTenantIdAndCriteria(TENANT, criteria, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().action()).isEqualTo("POST_CREATED");
    }

    @Test
    @DisplayName("SWR-087: search with entityType filter returns matching entries")
    void search_entityTypeFilter() {
        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria(null, "Tag", null, null, null, null);
        PageRequest pageable = PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<AuditLogEntry> result = adapter.findByTenantIdAndCriteria(TENANT, criteria, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(e -> "Tag".equals(e.entityType()));
    }

    @Test
    @DisplayName("SWR-087: search with actor filter returns matching entries")
    void search_actorFilter() {
        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria(null, null, "admin", null, null, null);
        PageRequest pageable = PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<AuditLogEntry> result = adapter.findByTenantIdAndCriteria(TENANT, criteria, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(e -> "admin".equals(e.actor()));
    }

    @Test
    @DisplayName("SWR-087: search with time range filter returns matching entries")
    void search_timeRangeFilter() {
        Instant now = Instant.now();
        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria(
                null, null, null, now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS), null);
        PageRequest pageable = PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<AuditLogEntry> result = adapter.findByTenantIdAndCriteria(TENANT, criteria, pageable);

        assertThat(result.getContent()).hasSize(4);
    }

    @Test
    @DisplayName("SWR-087: search with searchTerm matches across fields")
    void search_searchTermFilter() {
        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria(null, null, null, null, null, "Java");
        PageRequest pageable = PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<AuditLogEntry> result = adapter.findByTenantIdAndCriteria(TENANT, criteria, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().details()).contains("Java");
    }

    @Test
    @DisplayName("SWR-087: search with multiple filters combines them with AND")
    void search_multipleFilters() {
        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria("TAG_CREATED", "Tag", "admin", null, null, null);
        PageRequest pageable = PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<AuditLogEntry> result = adapter.findByTenantIdAndCriteria(TENANT, criteria, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().action()).isEqualTo("TAG_CREATED");
        assertThat(result.getContent().getFirst().actor()).isEqualTo("admin");
    }

    @Test
    @DisplayName("SWR-087: findDistinctActions returns unique sorted actions for tenant")
    void findDistinctActions() {
        List<String> actions = adapter.findDistinctActions(TENANT);

        assertThat(actions).containsExactly("POST_CREATED", "POST_UPDATED", "TAG_CREATED", "TAG_DELETED");
    }

    @Test
    @DisplayName("SWR-087: findDistinctEntityTypes returns unique sorted entity types for tenant")
    void findDistinctEntityTypes() {
        List<String> types = adapter.findDistinctEntityTypes(TENANT);

        assertThat(types).containsExactly("Post", "Tag");
    }

    @Test
    @DisplayName("SWR-087: findDistinctActors returns unique sorted actors for tenant")
    void findDistinctActors() {
        List<String> actors = adapter.findDistinctActors(TENANT);

        assertThat(actors).containsExactly("admin", "author1", "system");
    }
}
