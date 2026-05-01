package de.tomsblog.blogcontent.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.blogcontent.domain.model.*;
import de.tomsblog.shared.domain.AuthorId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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
@Import({JpaTranslationRepository.class, JpaPostRepository.class})
class JpaTranslationRepositoryIntegrationTest {

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
    private JpaTranslationRepository repository;

    @Autowired
    private JpaPostRepository postRepository;

    private final TenantId tenantId = TenantId.generate();
    private final AuthorId authorId = AuthorId.generate();
    private PostId postId;

    @BeforeEach
    void setUp() {
        Post post = Post.create(tenantId, authorId, "Test Post", "Content", PostLocale.german());
        Post saved = postRepository.save(post);
        postId = saved.getId();
    }

    @Test
    @DisplayName("SWR-005: Save and find translation by ID and tenant")
    void saveAndFindTranslation() {
        Translation translation =
                Translation.createManual(postId, tenantId, PostLocale.english(), "English Title", "English Content");

        Translation saved = repository.save(translation);

        Optional<Translation> found = repository.findByIdAndTenantId(saved.getId(), tenantId);
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("English Title");
        assertThat(found.get().getLocale()).isEqualTo(PostLocale.english());
        assertThat(found.get().getSource()).isEqualTo(TranslationSource.MANUAL);
        assertThat(found.get().getStatus()).isEqualTo(TranslationStatus.DRAFT);
    }

    @Test
    @DisplayName("SWR-003: Translation not found for different tenant")
    void translationNotFoundForDifferentTenant() {
        Translation translation = Translation.createManual(postId, tenantId, PostLocale.english(), "Title", "Content");
        Translation saved = repository.save(translation);

        TenantId otherTenant = TenantId.generate();
        Optional<Translation> found = repository.findByIdAndTenantId(saved.getId(), otherTenant);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("SWR-005: List all translations for post and tenant")
    void listAllByPostIdAndTenantId() {
        repository.save(Translation.createManual(postId, tenantId, PostLocale.english(), "EN Title", "EN Content"));
        repository.save(Translation.createManual(postId, tenantId, PostLocale.german(), "DE Titel", "DE Inhalt"));

        Post otherPost = Post.create(tenantId, authorId, "Other Post", "Other Content", PostLocale.german());
        Post savedOther = postRepository.save(otherPost);
        repository.save(Translation.createManual(
                savedOther.getId(), tenantId, PostLocale.english(), "Other Title", "Other Content"));

        List<Translation> translations = repository.findAllByPostIdAndTenantId(postId, tenantId);
        assertThat(translations).hasSize(2);
    }

    @Test
    @DisplayName("SWR-005: Delete translation by ID and tenant")
    void deleteByIdAndTenantId() {
        Translation translation = Translation.createManual(postId, tenantId, PostLocale.english(), "Title", "Content");
        Translation saved = repository.save(translation);

        repository.deleteByIdAndTenantId(saved.getId(), tenantId);

        Optional<Translation> found = repository.findByIdAndTenantId(saved.getId(), tenantId);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("SWR-004: Save AI-generated translation")
    void saveAiGeneratedTranslation() {
        Translation translation =
                Translation.createFromAi(postId, tenantId, PostLocale.english(), "AI Title", "AI Content");

        Translation saved = repository.save(translation);

        Optional<Translation> found = repository.findByIdAndTenantId(saved.getId(), tenantId);
        assertThat(found).isPresent();
        assertThat(found.get().getSource()).isEqualTo(TranslationSource.AI_GENERATED);
        assertThat(found.get().getStatus()).isEqualTo(TranslationStatus.REVIEW_PENDING);
    }
}
