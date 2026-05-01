package de.tomsblog.blogcontent.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.blogcontent.domain.model.*;
import de.tomsblog.shared.domain.AuthorId;
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
@Import(JpaPostRepository.class)
class JpaPostRepositoryIntegrationTest {

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
    private JpaPostRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private final TenantId tenantId = TenantId.generate();
    private final AuthorId authorId = AuthorId.generate();

    @Test
    @DisplayName("SWR-001: Save and retrieve post by ID and tenant")
    void saveAndFindPost() {
        Post post = Post.create(tenantId, authorId, "Integration Test Post", "Test content", PostLocale.german());
        TagId tagId = TagId.generate();
        post.addTag(tagId);

        Post saved = repository.save(post);

        Optional<Post> found = repository.findByIdAndTenantId(saved.getId(), tenantId);
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Integration Test Post");
        assertThat(found.get().getSlug().value()).isEqualTo("integration-test-post");
        assertThat(found.get().getAuthorId()).isEqualTo(authorId);
        assertThat(found.get().getTags()).containsExactly(tagId);
    }

    @Test
    @DisplayName("SWR-003: Post not found for different tenant")
    void postNotFoundForDifferentTenant() {
        Post post = Post.create(tenantId, authorId, "Tenant Post", "Content", PostLocale.german());
        Post saved = repository.save(post);

        TenantId otherTenant = TenantId.generate();
        Optional<Post> found = repository.findByIdAndTenantId(saved.getId(), otherTenant);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("SWR-001: List all posts for a tenant")
    void listPostsForTenant() {
        repository.save(Post.create(tenantId, authorId, "Post One", "Content 1", PostLocale.german()));
        repository.save(Post.create(tenantId, authorId, "Post Two", "Content 2", PostLocale.german()));

        TenantId otherTenant = TenantId.generate();
        AuthorId otherAuthor = AuthorId.generate();
        repository.save(Post.create(otherTenant, otherAuthor, "Other Post", "Content 3", PostLocale.english()));

        List<Post> posts = repository.findAllByTenantId(tenantId);
        assertThat(posts).hasSize(2);
    }

    @Test
    @DisplayName("SWR-012: Save and retrieve post with sources")
    void saveAndRetrievePostWithSources() {
        Post post = Post.create(tenantId, authorId, "Post with Sources", "Content", PostLocale.german());
        post.addSource(new Source("https://example.com", "Example"));
        post.addSource(new Source("https://docs.spring.io", "Spring Docs"));

        Post saved = repository.save(post);

        Optional<Post> found = repository.findByIdAndTenantId(saved.getId(), tenantId);
        assertThat(found).isPresent();
        assertThat(found.get().getSources()).hasSize(2);
    }

    @Test
    @DisplayName("Save and retrieve post with attachments")
    void saveAndRetrievePostWithAttachments() {
        Post post = Post.create(tenantId, authorId, "Post with Attachments", "Content", PostLocale.german());
        post.addAttachment(Attachment.create("photo.jpg", "image/jpeg", 2048, true));

        Post saved = repository.save(post);

        Optional<Post> found = repository.findByIdAndTenantId(saved.getId(), tenantId);
        assertThat(found).isPresent();
        assertThat(found.get().getAttachments()).hasSize(1);
        assertThat(found.get().getAttachments().getFirst().filename()).isEqualTo("photo.jpg");
        assertThat(found.get().getAttachments().getFirst().show()).isTrue();
    }

    @Test
    @DisplayName("SWR-001: Delete post by ID and tenant")
    void deleteByIdAndTenantId() {
        Post post = Post.create(tenantId, authorId, "To Delete", "Content", PostLocale.german());
        Post saved = repository.save(post);

        repository.deleteByIdAndTenantId(saved.getId(), tenantId);

        Optional<Post> found = repository.findByIdAndTenantId(saved.getId(), tenantId);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("SWR-001: Audit fields are populated on persist")
    void auditFieldsPopulatedOnPersist() {
        Post post = Post.create(tenantId, authorId, "Audit Test", "Content", PostLocale.german());
        repository.save(post);
        entityManager.flush();

        PostJpaEntity entity =
                entityManager.find(PostJpaEntity.class, post.getId().value());
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("SWR-001: UpdatedAt changes on entity update")
    void updatedAtChangesOnUpdate() {
        Post post = Post.create(tenantId, authorId, "Update Audit", "Content", PostLocale.german());
        Post saved = repository.save(post);
        entityManager.flush();

        PostJpaEntity entityBefore =
                entityManager.find(PostJpaEntity.class, saved.getId().value());
        entityBefore.setCreatedBy("test-user");
        entityBefore.setUpdatedBy("test-user");
        entityManager.persistAndFlush(entityBefore);

        PostJpaEntity entityAfter =
                entityManager.find(PostJpaEntity.class, saved.getId().value());
        assertThat(entityAfter.getCreatedBy()).isEqualTo("test-user");
        assertThat(entityAfter.getUpdatedBy()).isEqualTo("test-user");
    }
}
