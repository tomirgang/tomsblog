package de.tomsblog.feed.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.feed.application.port.outbound.FeedEntryRepository;
import de.tomsblog.feed.domain.model.FeedEntry;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
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
@Import(SpringDataFeedEntryRepository.class)
class SpringDataFeedEntryRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("feed_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/feed");
    }

    @Autowired
    private FeedEntryRepository repository;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID postId = UUID.randomUUID();

    @Test
    @DisplayName("SWR-090: saves and retrieves feed entry by tenantId and postId")
    void savesAndRetrievesByTenantAndPost() {
        FeedEntry entry = FeedEntry.create(tenantId, postId, "my-post", "de", Instant.now());

        repository.save(entry);
        Optional<FeedEntry> found = repository.findByTenantIdAndPostId(tenantId, postId);

        assertThat(found).isPresent();
        assertThat(found.get().getPostId()).isEqualTo(postId);
        assertThat(found.get().getTenantId()).isEqualTo(tenantId);
        assertThat(found.get().getSlug()).isEqualTo("my-post");
    }

    @Test
    @DisplayName("SWR-090: returns empty when no entry exists for tenantId and postId")
    void returnsEmptyWhenNotFound() {
        Optional<FeedEntry> found = repository.findByTenantIdAndPostId(tenantId, UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("SWR-090: updates existing entry on save")
    void updatesExistingEntry() {
        FeedEntry entry = FeedEntry.create(tenantId, postId, "old-slug", "de", Instant.now());
        repository.save(entry);

        entry.update("New Title", "new-slug", "en");
        repository.save(entry);

        Optional<FeedEntry> found = repository.findByTenantIdAndPostId(tenantId, postId);
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("New Title");
        assertThat(found.get().getSlug()).isEqualTo("new-slug");
        assertThat(found.get().getLocale()).isEqualTo("en");
    }

    @Test
    @DisplayName("SWR-090: isolates feed entries by tenant")
    void isolatesByTenant() {
        UUID otherTenantId = UUID.randomUUID();
        FeedEntry entry1 = FeedEntry.create(tenantId, postId, "slug-1", "de", Instant.now());
        FeedEntry entry2 = FeedEntry.create(otherTenantId, postId, "slug-2", "en", Instant.now());
        repository.save(entry1);
        repository.save(entry2);

        Optional<FeedEntry> found = repository.findByTenantIdAndPostId(tenantId, postId);
        assertThat(found).isPresent();
        assertThat(found.get().getSlug()).isEqualTo("slug-1");

        Optional<FeedEntry> otherFound = repository.findByTenantIdAndPostId(otherTenantId, postId);
        assertThat(otherFound).isPresent();
        assertThat(otherFound.get().getSlug()).isEqualTo("slug-2");
    }
}
