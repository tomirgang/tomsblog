package de.tomsblog.blogcontent.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.blogcontent.domain.model.*;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.Optional;
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
@Import(JpaPostRepository.class)
class JpaPostRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private JpaPostRepository repository;

    private final TenantId tenantId = TenantId.generate();

    @Test
    @DisplayName("SWR-001: Save and retrieve post by ID and tenant")
    void saveAndFindPost() {
        Post post = Post.create(tenantId, "Integration Test Post", "Test content", PostLocale.german());
        post.addTag(new Tag("test"));

        Post saved = repository.save(post);

        Optional<Post> found = repository.findByIdAndTenantId(saved.getId(), tenantId);
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Integration Test Post");
        assertThat(found.get().getSlug().value()).isEqualTo("integration-test-post");
        assertThat(found.get().getTags()).containsExactly(new Tag("test"));
    }

    @Test
    @DisplayName("SWR-003: Post not found for different tenant")
    void postNotFoundForDifferentTenant() {
        Post post = Post.create(tenantId, "Tenant Post", "Content", PostLocale.german());
        Post saved = repository.save(post);

        TenantId otherTenant = TenantId.generate();
        Optional<Post> found = repository.findByIdAndTenantId(saved.getId(), otherTenant);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("SWR-001: List all posts for a tenant")
    void listPostsForTenant() {
        repository.save(Post.create(tenantId, "Post One", "Content 1", PostLocale.german()));
        repository.save(Post.create(tenantId, "Post Two", "Content 2", PostLocale.german()));

        TenantId otherTenant = TenantId.generate();
        repository.save(Post.create(otherTenant, "Other Post", "Content 3", PostLocale.english()));

        List<Post> posts = repository.findAllByTenantId(tenantId);
        assertThat(posts).hasSize(2);
    }
}
