package de.tomsblog.feed.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeedEntryTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID postId = UUID.randomUUID();

    @Test
    @DisplayName("SWR-090: create() generates id and sets fields from published event")
    void createGeneratesIdAndSetsFields() {
        Instant publishedAt = Instant.now();

        FeedEntry entry = FeedEntry.create(tenantId, postId, "my-post", "de", publishedAt);

        assertThat(entry.getId()).isNotNull();
        assertThat(entry.getTenantId()).isEqualTo(tenantId);
        assertThat(entry.getPostId()).isEqualTo(postId);
        assertThat(entry.getSlug()).isEqualTo("my-post");
        assertThat(entry.getLocale()).isEqualTo("de");
        assertThat(entry.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(entry.getUpdatedAt()).isNotNull();
        assertThat(entry.getTitle()).isNull();
    }

    @Test
    @DisplayName("SWR-090: update() changes title, slug, locale and updatedAt")
    void updateChangesFields() {
        FeedEntry entry = FeedEntry.create(tenantId, postId, "old-slug", "de", Instant.now());
        Instant beforeUpdate = entry.getUpdatedAt();

        entry.update("New Title", "new-slug", "en");

        assertThat(entry.getTitle()).isEqualTo("New Title");
        assertThat(entry.getSlug()).isEqualTo("new-slug");
        assertThat(entry.getLocale()).isEqualTo("en");
        assertThat(entry.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("SWR-090: reconstitute() restores all fields from persistence")
    void reconstituteRestoresAllFields() {
        UUID id = UUID.randomUUID();
        Instant publishedAt = Instant.parse("2026-01-15T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-16T12:00:00Z");

        FeedEntry entry = FeedEntry.reconstitute(id, tenantId, postId, "Title", "slug", "de", publishedAt, updatedAt);

        assertThat(entry.getId()).isEqualTo(id);
        assertThat(entry.getTenantId()).isEqualTo(tenantId);
        assertThat(entry.getPostId()).isEqualTo(postId);
        assertThat(entry.getTitle()).isEqualTo("Title");
        assertThat(entry.getSlug()).isEqualTo("slug");
        assertThat(entry.getLocale()).isEqualTo("de");
        assertThat(entry.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(entry.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("SWR-090: create() rejects null id fields")
    void createRejectsNullTenantId() {
        assertThatThrownBy(() -> FeedEntry.create(null, postId, "slug", "de", Instant.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    @DisplayName("SWR-090: create() rejects null postId")
    void createRejectsNullPostId() {
        assertThatThrownBy(() -> FeedEntry.create(tenantId, null, "slug", "de", Instant.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("postId");
    }

    @Test
    @DisplayName("SWR-090: create() rejects null slug")
    void createRejectsNullSlug() {
        assertThatThrownBy(() -> FeedEntry.create(tenantId, postId, null, "de", Instant.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("slug");
    }
}
