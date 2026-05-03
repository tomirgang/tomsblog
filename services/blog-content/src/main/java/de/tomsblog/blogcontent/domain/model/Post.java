package de.tomsblog.blogcontent.domain.model;

import de.tomsblog.blogcontent.domain.event.PostCreatedEvent;
import de.tomsblog.blogcontent.domain.event.PostPublishedEvent;
import de.tomsblog.blogcontent.domain.event.PostUpdatedEvent;
import de.tomsblog.shared.domain.AggregateRoot;
import de.tomsblog.shared.domain.AuthorId;
import de.tomsblog.shared.tenant.TenantId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Blog post aggregate root.
 *
 * @req SWR-001
 * @req SWR-003
 * @req SWR-012
 */
public class Post extends AggregateRoot {

    private final PostId id;
    private final TenantId tenantId;
    private final AuthorId authorId;
    private String title;
    private Slug slug;
    private String content;
    private ContentType contentType;
    private PostStatus status;
    private PostLocale locale;
    private final Set<TagId> tags;
    private final List<Source> sources;
    private final List<Attachment> attachments;
    private Instant publishedAt;
    private String socialMediaTitle;
    private String socialMediaSummary;

    private Post(
            PostId id,
            TenantId tenantId,
            AuthorId authorId,
            String title,
            Slug slug,
            String content,
            ContentType contentType,
            PostLocale locale) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.authorId = Objects.requireNonNull(authorId);
        this.title = Objects.requireNonNull(title);
        this.slug = Objects.requireNonNull(slug);
        this.content = Objects.requireNonNull(content);
        this.contentType = Objects.requireNonNull(contentType);
        this.locale = Objects.requireNonNull(locale);
        this.status = PostStatus.DRAFT;
        this.tags = new HashSet<>();
        this.sources = new ArrayList<>();
        this.attachments = new ArrayList<>();
    }

    /**
     * Creates a new draft post for the given tenant and author.
     *
     * @req SWR-001
     * @req SWR-003
     * @req SWR-035
     */
    public static Post create(
            TenantId tenantId,
            AuthorId authorId,
            String title,
            String content,
            ContentType contentType,
            PostLocale locale) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        Objects.requireNonNull(authorId, "AuthorId must not be null");
        PostId id = PostId.generate();
        Slug slug = Slug.fromTitle(title);
        Post post = new Post(id, tenantId, authorId, title, slug, content, contentType, locale);
        post.registerEvent(PostCreatedEvent.of(id, tenantId));
        return post;
    }

    /**
     * Convenience overload that defaults to HTML content type.
     *
     * @req SWR-001
     * @req SWR-003
     */
    public static Post create(TenantId tenantId, AuthorId authorId, String title, String content, PostLocale locale) {
        return create(tenantId, authorId, title, content, ContentType.HTML, locale);
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
        registerEvent(PostUpdatedEvent.of(this.id, this.tenantId));
    }

    public void updateSocialMedia(String socialMediaTitle, String socialMediaSummary) {
        this.socialMediaTitle = socialMediaTitle;
        this.socialMediaSummary = socialMediaSummary;
    }

    public void addTag(TagId tagId) {
        tags.add(tagId);
    }

    public void removeTag(TagId tagId) {
        tags.remove(tagId);
    }

    /**
     * @req SWR-012
     */
    public void addSource(Source source) {
        Objects.requireNonNull(source, "Source must not be null");
        sources.add(source);
    }

    public void removeSource(Source source) {
        sources.remove(source);
    }

    public void addAttachment(Attachment attachment) {
        Objects.requireNonNull(attachment, "Attachment must not be null");
        attachments.add(attachment);
    }

    public void removeAttachment(AttachmentId attachmentId) {
        attachments.removeIf(a -> a.id().equals(attachmentId));
    }

    public PostId getId() {
        return id;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public AuthorId getAuthorId() {
        return authorId;
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

    public ContentType getContentType() {
        return contentType;
    }

    public PostStatus getStatus() {
        return status;
    }

    public PostLocale getLocale() {
        return locale;
    }

    public Set<TagId> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public List<Source> getSources() {
        return Collections.unmodifiableList(sources);
    }

    public List<Attachment> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getSocialMediaTitle() {
        return socialMediaTitle;
    }

    public String getSocialMediaSummary() {
        return socialMediaSummary;
    }

    /**
     * Returns the effective social media title: the explicit value if set, otherwise the post title.
     */
    public String getEffectiveSocialMediaTitle() {
        return (socialMediaTitle != null && !socialMediaTitle.isBlank()) ? socialMediaTitle : title;
    }

    /**
     * Returns the effective social media summary: the explicit value if set,
     * otherwise the first paragraph of the post content.
     */
    public String getEffectiveSocialMediaSummary() {
        if (socialMediaSummary != null && !socialMediaSummary.isBlank()) {
            return socialMediaSummary;
        }
        return extractFirstParagraph(content);
    }

    private static String extractFirstParagraph(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        int index = text.indexOf("\n\n");
        if (index > 0) {
            return text.substring(0, index).strip();
        }
        return text.strip();
    }

    // Reconstitution from persistence (no events fired)
    public static Post reconstitute(
            PostId id,
            TenantId tenantId,
            AuthorId authorId,
            String title,
            Slug slug,
            String content,
            ContentType contentType,
            PostStatus status,
            PostLocale locale,
            Set<TagId> tags,
            List<Source> sources,
            List<Attachment> attachments,
            Instant publishedAt,
            String socialMediaTitle,
            String socialMediaSummary) {
        Post post = new Post(id, tenantId, authorId, title, slug, content, contentType, locale);
        post.status = status;
        post.publishedAt = publishedAt;
        post.socialMediaTitle = socialMediaTitle;
        post.socialMediaSummary = socialMediaSummary;
        post.tags.addAll(tags);
        post.sources.addAll(sources);
        post.attachments.addAll(attachments);
        return post;
    }
}
