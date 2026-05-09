package de.tomsblog.feed.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feed_entries", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "post_id"}))
public class FeedEntryJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "post_id", nullable = false, updatable = false)
    private UUID postId;

    @Column
    private String title;

    @Column(nullable = false)
    private String slug;

    @Column
    private String locale;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FeedEntryJpaEntity() {}

    public FeedEntryJpaEntity(
            UUID id,
            UUID tenantId,
            UUID postId,
            String title,
            String slug,
            String locale,
            Instant publishedAt,
            Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.postId = postId;
        this.title = title;
        this.slug = slug;
        this.locale = locale;
        this.publishedAt = publishedAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
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
