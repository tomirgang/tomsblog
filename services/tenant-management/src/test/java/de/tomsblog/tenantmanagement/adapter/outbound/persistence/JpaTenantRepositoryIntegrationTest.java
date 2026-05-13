package de.tomsblog.tenantmanagement.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.tenantmanagement.domain.model.LoginMode;
import de.tomsblog.tenantmanagement.domain.model.Tenant;
import de.tomsblog.tenantmanagement.domain.model.TenantStatus;
import java.util.Set;
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
@Import(JpaTenantRepository.class)
class JpaTenantRepositoryIntegrationTest {

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
    private JpaTenantRepository repository;

    @Test
    @DisplayName("SWR-072: save and findByTenantId round-trip")
    void saveAndFind() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.create(tenantId, "test-blog", "Test Blog");
        repository.save(tenant);

        var found = repository.findByTenantId(tenantId);
        assertThat(found).isPresent();
        assertThat(found.get().getSlug()).isEqualTo("test-blog");
        assertThat(found.get().getDisplayName()).isEqualTo("Test Blog");
        assertThat(found.get().getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(found.get().getLoginMode()).isEqualTo(LoginMode.INTERNAL);
    }

    @Test
    @DisplayName("SWR-072: findByTenantId returns empty for unknown tenant")
    void findByTenantIdNotFound() {
        assertThat(repository.findByTenantId(TenantId.generate())).isEmpty();
    }

    @Test
    @DisplayName("SWR-072: findAll returns all tenants")
    void findAllReturnsAll() {
        repository.save(Tenant.create(TenantId.generate(), "a", "A"));
        repository.save(Tenant.create(TenantId.generate(), "b", "B"));

        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("SWR-072: save persists auto-approve domains")
    void saveWithDomains() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.reconstitute(
                tenantId,
                "slug",
                "Name",
                null,
                TenantStatus.ACTIVE,
                LoginMode.BOTH,
                true,
                Set.of("example.com", "corp.org"),
                null,
                null,
                null,
                null,
                null,
                null,
                "READER",
                null,
                null,
                false,
                java.util.Map.of());
        repository.save(tenant);

        var found = repository.findByTenantId(tenantId);
        assertThat(found.get().getAutoApproveEmailDomains()).containsExactlyInAnyOrder("example.com", "corp.org");
    }

    @Test
    @DisplayName("SWR-072: save persists OIDC configuration")
    void saveWithOidcConfig() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.create(tenantId, "oidc-blog", "OIDC Blog");
        tenant.updateOidcSettings("https://issuer.com", "client-id", "client-secret", null, false, java.util.Map.of());
        repository.save(tenant);

        var found = repository.findByTenantId(tenantId);
        assertThat(found.get().getOidcIssuerUrl()).isEqualTo("https://issuer.com");
        assertThat(found.get().getOidcClientId()).isEqualTo("client-id");
        assertThat(found.get().getOidcClientSecret()).isEqualTo("client-secret");
    }

    @Test
    @DisplayName("SWR-072: save persists legal content")
    void saveWithLegalContent() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.create(tenantId, "legal-blog", "Legal Blog");
        tenant.updateLegalSettings("<p>Impressum</p>", "<p>Datenschutz</p>");
        repository.save(tenant);

        var found = repository.findByTenantId(tenantId);
        assertThat(found.get().getImpressumContent()).isEqualTo("<p>Impressum</p>");
        assertThat(found.get().getPrivacyPolicyContent()).isEqualTo("<p>Datenschutz</p>");
    }
}
