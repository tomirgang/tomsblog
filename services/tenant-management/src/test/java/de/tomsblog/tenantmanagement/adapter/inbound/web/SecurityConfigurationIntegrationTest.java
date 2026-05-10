package de.tomsblog.tenantmanagement.adapter.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;

import de.tomsblog.tenantmanagement.TenantManagementConfiguration;
import de.tomsblog.tenantmanagement.adapter.outbound.persistence.JpaAuditLogger;
import de.tomsblog.tenantmanagement.adapter.outbound.persistence.JpaTenantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.session.store-type=none", "blog.admin.password=test-password", "grpc.server.port=0"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class SecurityConfigurationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("tenant_management_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SecurityFilterChain[] filterChains;

    @Autowired
    private TenantManagementConfiguration tenantManagementConfiguration;

    @Autowired
    private JpaTenantRepository tenantRepository;

    @Autowired
    private JpaAuditLogger auditLogger;

    @Test
    @DisplayName("SWR-073: security configuration creates two filter chains")
    void twoFilterChains() {
        assertThat(filterChains).hasSize(2);
    }

    @Test
    @DisplayName("SWR-073: tenantManagementUseCase bean is created")
    void tenantManagementUseCaseBeanCreated() {
        var useCase = tenantManagementConfiguration.tenantManagementUseCase(tenantRepository, auditLogger);
        assertThat(useCase).isNotNull();
    }
}
