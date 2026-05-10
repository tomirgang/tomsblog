package de.tomsblog.feed.domain.model;

import de.tomsblog.shared.tenant.TenantId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Denormalized feed entry representing a published blog post.
 * One entry per (tenantId, postId) combination.
 */
public class FeedEntry {

    private final UUID id;
    private final TenantId tenantId;
    private final UUID postId;
    private String title;
    private String slug;
    private String locale;
    private Instant publishedAt;
    private Instant updatedAt;

    private FeedEntry(
            UUID id,
            TenantId tenantId,
            UUID postId,
            String title,
            String slug,
            String locale,
            Instant publishedAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.postId = Objects.requireNonNull(postId, "postId must not be null");
        this.title = title;
        this.slug = Objects.requireNonNull(slug, "slug must not be null");
        this.locale = locale;
        this.publishedAt = publishedAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Creates a new feed entry from a post-published event.
     */
    public static FeedEntry create(TenantId tenantId, UUID postId, String slug, String locale, Instant publishedAt) {
        return new FeedEntry(UUID.randomUUID(), tenantId, postId, null, slug, locale, publishedAt, Instant.now());
    }

    /**
     * Reconstructs a feed entry from persistence.
     */
    public static FeedEntry reconstitute(
            UUID id,
            TenantId tenantId,
            UUID postId,
            String title,
            String slug,
            String locale,
            Instant publishedAt,
            Instant updatedAt) {
        return new FeedEntry(id, tenantId, postId, title, slug, locale, publishedAt, updatedAt);
    }

    /**
     * Updates this feed entry with new post metadata.
     */
    public void update(String title, String slug, String locale) {
        this.title = title;
        this.slug = slug;
        this.locale = locale;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public UUID getPostId() {
        return postId;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public String getLocale() {
        return locale;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
