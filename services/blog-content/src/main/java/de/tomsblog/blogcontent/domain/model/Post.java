package de.tomsblog.blogcontent.domain.model;

import de.tomsblog.shared.domain.AggregateRoot;
import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.blogcontent.domain.event.PostCreatedEvent;
import de.tomsblog.blogcontent.domain.event.PostPublishedEvent;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Blog post aggregate root.
 *
 * @req SWR-001
 * @req SWR-003
 */
public class Post extends AggregateRoot {

    private final PostId id;
    private final TenantId tenantId;
    private String title;
    private Slug slug;
    private String content;
    private PostStatus status;
    private PostLocale locale;
    private final Set<Tag> tags;
    private Instant publishedAt;

    private Post(PostId id, TenantId tenantId, String title, Slug slug, String content, PostLocale locale) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.title = Objects.requireNonNull(title);
        this.slug = Objects.requireNonNull(slug);
        this.content = Objects.requireNonNull(content);
        this.locale = Objects.requireNonNull(locale);
        this.status = PostStatus.DRAFT;
        this.tags = new HashSet<>();
    }

    /**
     * Creates a new draft post for the given tenant.
     *
     * @req SWR-001
     * @req SWR-003
     */
    public static Post create(TenantId tenantId, String title, String content, PostLocale locale) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        PostId id = PostId.generate();
        Slug slug = Slug.fromTitle(title);
        Post post = new Post(id, tenantId, title, slug, content, locale);
        post.registerEvent(PostCreatedEvent.of(id, tenantId));
        return post;
    }

    /**
     * Publishes this post. Idempotency: throws if already published.
     *
     * @req SWR-002
     */
    public void publish() {
        if (this.status == PostStatus.PUBLISHED) {
            throw new IllegalStateException("Post is already published");
        }
        this.status = PostStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        registerEvent(PostPublishedEvent.of(this.id, this.tenantId));
    }

    public void archive() {
        this.status = PostStatus.ARCHIVED;
    }

    public void updateContent(String title, String content) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        this.title = title;
        this.slug = Slug.fromTitle(title);
        this.content = content;
    }

    public void addTag(Tag tag) {
        tags.add(tag);
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
    }

    public PostId getId() {
        return id;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public String getTitle() {
        return title;
    }

    public Slug getSlug() {
        return slug;
    }

    public String getContent() {
        return content;
    }

    public PostStatus getStatus() {
        return status;
    }

    public PostLocale getLocale() {
        return locale;
    }

    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    // Reconstitution from persistence (no events fired)
    public static Post reconstitute(
            PostId id,
            TenantId tenantId,
            String title,
            Slug slug,
            String content,
            PostStatus status,
            PostLocale locale,
            Set<Tag> tags,
            Instant publishedAt) {
        Post post = new Post(id, tenantId, title, slug, content, locale);
        post.status = status;
        post.publishedAt = publishedAt;
        post.tags.addAll(tags);
        return post;
    }
}
