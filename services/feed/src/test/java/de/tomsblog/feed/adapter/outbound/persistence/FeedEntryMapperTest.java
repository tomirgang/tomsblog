package de.tomsblog.feed.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.feed.domain.model.FeedEntry;
import de.tomsblog.shared.tenant.TenantId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeedEntryMapperTest {

    @Test
    @DisplayName("SWR-090: maps domain to JPA entity correctly")
    void mapsToJpa() {
        TenantId tenantId = TenantId.generate();
        UUID postId = UUID.randomUUID();
        FeedEntry domain = FeedEntry.create(tenantId, postId, "slug", "de", Instant.now());

        FeedEntryJpaEntity jpa = FeedEntryMapper.toJpa(domain);

        assertThat(jpa.getId()).isEqualTo(domain.getId());
        assertThat(jpa.getTenantId()).isEqualTo(tenantId.value());
        assertThat(jpa.getPostId()).isEqualTo(postId);
        assertThat(jpa.getSlug()).isEqualTo("slug");
        assertThat(jpa.getLocale()).isEqualTo("de");
        assertThat(jpa.getPublishedAt()).isEqualTo(domain.getPublishedAt());
        assertThat(jpa.getUpdatedAt()).isEqualTo(domain.getUpdatedAt());
    }

    @Test
    @DisplayName("SWR-090: maps JPA entity to domain correctly")
    void mapsToDomain() {
        UUID id = UUID.randomUUID();
        UUID tenantUuid = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Instant publishedAt = Instant.now();
        Instant updatedAt = Instant.now();
        FeedEntryJpaEntity jpa =
                new FeedEntryJpaEntity(id, tenantUuid, postId, "Title", "slug", "de", publishedAt, updatedAt);

        FeedEntry domain = FeedEntryMapper.toDomain(jpa);

        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getTenantId()).isEqualTo(TenantId.of(tenantUuid));
        assertThat(domain.getPostId()).isEqualTo(postId);
        assertThat(domain.getTitle()).isEqualTo("Title");
        assertThat(domain.getSlug()).isEqualTo("slug");
        assertThat(domain.getLocale()).isEqualTo("de");
        assertThat(domain.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(domain.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("SWR-090: roundtrip domain -> JPA -> domain preserves all fields")
    void roundtripPreservesFields() {
        TenantId tenantId = TenantId.generate();
        UUID postId = UUID.randomUUID();
        FeedEntry original = FeedEntry.create(tenantId, postId, "test-slug", "en", Instant.now());

        FeedEntry roundtripped = FeedEntryMapper.toDomain(FeedEntryMapper.toJpa(original));

        assertThat(roundtripped.getId()).isEqualTo(original.getId());
        assertThat(roundtripped.getTenantId()).isEqualTo(original.getTenantId());
        assertThat(roundtripped.getPostId()).isEqualTo(original.getPostId());
        assertThat(roundtripped.getTitle()).isEqualTo(original.getTitle());
        assertThat(roundtripped.getSlug()).isEqualTo(original.getSlug());
        assertThat(roundtripped.getLocale()).isEqualTo(original.getLocale());
        assertThat(roundtripped.getPublishedAt()).isEqualTo(original.getPublishedAt());
        assertThat(roundtripped.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
    }
}
