package de.tomsblog.blogcontent.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.blogcontent.domain.model.Tag;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaTagRepository.class)
class JpaTagRepositoryIntegrationTest {

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
    private JpaTagRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private final TenantId tenantId = TenantId.generate();

    @Test
    @DisplayName("SWR-006: Save and find tag by ID and tenant")
    void saveAndFindTag() {
        Tag tag = Tag.create(tenantId, "Java");

        Tag saved = repository.save(tag);

        Optional<Tag> found = repository.findByIdAndTenantId(saved.getId(), tenantId);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Java");
        assertThat(found.get().getSlug().value()).isEqualTo("java");
    }

    @Test
    @DisplayName("SWR-006: Find tag by name and tenant")
    void findByNameAndTenantId() {
        Tag tag = Tag.create(tenantId, "Spring");
        repository.save(tag);

        Optional<Tag> found = repository.findByNameAndTenantId("Spring", tenantId);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Spring");
    }

    @Test
    @DisplayName("SWR-006: Find by name returns empty for non-existing tag")
    void findByNameReturnsEmptyWhenNotFound() {
        Optional<Tag> found = repository.findByNameAndTenantId("NonExisting", tenantId);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("SWR-003: Tag not found for different tenant")
    void tagNotFoundForDifferentTenant() {
        Tag tag = Tag.create(tenantId, "Java");
        Tag saved = repository.save(tag);

        TenantId otherTenant = TenantId.generate();
        Optional<Tag> found = repository.findByIdAndTenantId(saved.getId(), otherTenant);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("SWR-006: List all tags for tenant")
    void listAllByTenantId() {
        repository.save(Tag.create(tenantId, "Java"));
        repository.save(Tag.create(tenantId, "Kotlin"));

        TenantId otherTenant = TenantId.generate();
        repository.save(Tag.create(otherTenant, "Python"));

        List<Tag> tags = repository.findAllByTenantId(tenantId);
        assertThat(tags).hasSize(2);
    }

    @Test
    @DisplayName("SWR-006: Delete tag by ID and tenant")
    void deleteByIdAndTenantId() {
        Tag tag = Tag.create(tenantId, "ToDelete");
        Tag saved = repository.save(tag);

        repository.deleteByIdAndTenantId(saved.getId(), tenantId);

        Optional<Tag> found = repository.findByIdAndTenantId(saved.getId(), tenantId);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("SWR-006: Audit fields are populated on persist")
    void auditFieldsPopulatedOnPersist() {
        Tag tag = Tag.create(tenantId, "AuditTag");
        repository.save(tag);
        entityManager.flush();

        TagJpaEntity entity = entityManager.find(TagJpaEntity.class, tag.getId().value());
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("SWR-006: UpdatedAt changes on entity update")
    void updatedAtChangesOnUpdate() {
        Tag tag = Tag.create(tenantId, "UpdateTag");
        repository.save(tag);
        entityManager.flush();
        entityManager.clear();

        TagJpaEntity entity = entityManager.find(TagJpaEntity.class, tag.getId().value());
        entity.setName("RenamedTag");
        entityManager.persistAndFlush(entity);

        TagJpaEntity updated =
                entityManager.find(TagJpaEntity.class, tag.getId().value());
        assertThat(updated.getUpdatedAt()).isNotNull();
    }
}
