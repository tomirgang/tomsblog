package de.tomsblog.usermanagement.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
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
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaTenantSettingsRepository.class)
class JpaTenantSettingsRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JpaTenantSettingsRepository repository;

    @Test
    @DisplayName("SWR-044: save and findByTenantId roundtrip")
    void saveAndFind() {
        var tenantId = TenantId.generate();
        var settings = TenantSettings.reconstitute(
                tenantId,
                LoginMode.OIDC,
                true,
                Set.of("example.com", "test.org"),
                "Toms Blog",
                null,
                null,
                null,
                null,
                null,
                null);

        repository.save(settings);
        var found = repository.findByTenantId(tenantId);

        assertThat(found).isPresent();
        assertThat(found.get().getTenantId()).isEqualTo(tenantId);
        assertThat(found.get().getLoginMode()).isEqualTo(LoginMode.OIDC);
        assertThat(found.get().isAutoApproveOidc()).isTrue();
        assertThat(found.get().getAutoApproveEmailDomains()).containsExactlyInAnyOrder("example.com", "test.org");
    }

    @Test
    @DisplayName("SWR-044: findByTenantId returns empty when not found")
    void findByTenantIdReturnsEmpty() {
        var result = repository.findByTenantId(TenantId.generate());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("SWR-044: save updates existing settings")
    void saveUpdatesExisting() {
        var tenantId = TenantId.generate();
        var settings = TenantSettings.reconstitute(
                tenantId, LoginMode.BOTH, false, Set.of(), "Toms Blog", null, null, null, null, null, null);
        repository.save(settings);

        var updated = TenantSettings.reconstitute(
                tenantId,
                LoginMode.INTERNAL,
                true,
                Set.of("corp.com"),
                "Toms Blog",
                null,
                null,
                null,
                null,
                null,
                null);
        repository.save(updated);

        var found = repository.findByTenantId(tenantId);
        assertThat(found).isPresent();
        assertThat(found.get().getLoginMode()).isEqualTo(LoginMode.INTERNAL);
        assertThat(found.get().isAutoApproveOidc()).isTrue();
        assertThat(found.get().getAutoApproveEmailDomains()).containsExactly("corp.com");
    }

    @Test
    @DisplayName("SWR-045: email domains are persisted correctly")
    void emailDomainsPersistedCorrectly() {
        var tenantId = TenantId.generate();
        var settings = TenantSettings.reconstitute(
                tenantId,
                LoginMode.BOTH,
                false,
                Set.of("a.com", "b.org", "c.net"),
                "Toms Blog",
                null,
                null,
                null,
                null,
                null,
                null);

        repository.save(settings);
        var found = repository.findByTenantId(tenantId);

        assertThat(found).isPresent();
        assertThat(found.get().getAutoApproveEmailDomains()).hasSize(3);
        assertThat(found.get().getAutoApproveEmailDomains()).containsExactlyInAnyOrder("a.com", "b.org", "c.net");
    }

    @Test
    @DisplayName("SWR-053: findAll returns all saved tenants")
    void findAllReturnsAllTenants() {
        var tenantId1 = TenantId.generate();
        var tenantId2 = TenantId.generate();
        repository.save(TenantSettings.reconstitute(
                tenantId1, LoginMode.BOTH, false, Set.of(), "Blog 1", null, null, null, null, null, null));
        repository.save(TenantSettings.reconstitute(
                tenantId2, LoginMode.OIDC, true, Set.of("x.com"), "Blog 2", "A tagline", null, null, null, null, null));

        var all = repository.findAll();

        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
        assertThat(all.stream().map(s -> s.getTenantId().value())).contains(tenantId1.value(), tenantId2.value());
    }

    @Test
    @DisplayName("SWR-050: branding fields are persisted correctly")
    void brandingFieldsPersistedCorrectly() {
        var tenantId = TenantId.generate();
        var settings = TenantSettings.reconstitute(
                tenantId,
                LoginMode.BOTH,
                false,
                Set.of(),
                "My Custom Blog",
                "A great tagline",
                null,
                null,
                null,
                null,
                null);

        repository.save(settings);
        var found = repository.findByTenantId(tenantId);

        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("My Custom Blog");
        assertThat(found.get().getTagline()).isEqualTo("A great tagline");
    }
}
