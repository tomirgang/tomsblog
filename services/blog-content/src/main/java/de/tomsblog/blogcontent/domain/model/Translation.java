package de.tomsblog.blogcontent.domain.model;

import de.tomsblog.shared.domain.AggregateRoot;
import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.blogcontent.domain.event.TranslationCreatedEvent;
import de.tomsblog.blogcontent.domain.event.TranslationApprovedEvent;
import java.time.Instant;
import java.util.Objects;

/**
 * Translation aggregate root. Represents a translated version of a post.
 * Has its own lifecycle (DRAFT -> REVIEW_PENDING -> APPROVED/REJECTED).
 *
 * @req SWR-004
 * @req SWR-005
 */
public class Translation extends AggregateRoot {

    private final TranslationId id;
    private final PostId postId;
    private final TenantId tenantId;
    private final PostLocale locale;
    private String title;
    private String content;
    private TranslationStatus status;
    private final TranslationSource source;
    private final Instant translatedAt;

    private Translation(TranslationId id, PostId postId, TenantId tenantId, PostLocale locale,
            String title, String content, TranslationStatus status,
            TranslationSource source, Instant translatedAt) {
        this.id = Objects.requireNonNull(id);
        this.postId = Objects.requireNonNull(postId);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.locale = Objects.requireNonNull(locale);
        this.title = Objects.requireNonNull(title);
        this.content = Objects.requireNonNull(content);
        this.status = Objects.requireNonNull(status);
        this.source = Objects.requireNonNull(source);
        this.translatedAt = Objects.requireNonNull(translatedAt);
    }

    /**
     * Creates a manual translation (immediately in DRAFT status).
     *
     * @req SWR-005
     */
    public static Translation createManual(PostId postId, TenantId tenantId, PostLocale locale,
            String title, String content) {
        validateContent(title, content);
        TranslationId id = TranslationId.generate();
        Translation translation = new Translation(id, postId, tenantId, locale, title, content,
                TranslationStatus.DRAFT, TranslationSource.MANUAL, Instant.now());
        translation.registerEvent(TranslationCreatedEvent.of(id, postId, tenantId, locale, TranslationSource.MANUAL));
        return translation;
    }

    /**
     * Creates an AI-generated translation (starts in REVIEW_PENDING status).
     *
     * @req SWR-004
     */
    public static Translation createFromAi(PostId postId, TenantId tenantId, PostLocale locale,
            String title, String content) {
        validateContent(title, content);
        TranslationId id = TranslationId.generate();
        Translation translation = new Translation(id, postId, tenantId, locale, title, content,
                TranslationStatus.REVIEW_PENDING, TranslationSource.AI_GENERATED, Instant.now());
        translation.registerEvent(
                TranslationCreatedEvent.of(id, postId, tenantId, locale, TranslationSource.AI_GENERATED));
        return translation;
    }

    /**
     * Approves this translation. Only allowed from DRAFT or REVIEW_PENDING.
     *
     * @req SWR-004
     */
    public void approve() {
        if (this.status == TranslationStatus.APPROVED) {
            throw new IllegalStateException("Translation is already approved");
        }
        if (this.status == TranslationStatus.REJECTED) {
            throw new IllegalStateException("Cannot approve a rejected translation");
        }
        this.status = TranslationStatus.APPROVED;
        registerEvent(TranslationApprovedEvent.of(this.id, this.postId, this.tenantId, this.locale));
    }

    public void reject() {
        if (this.status == TranslationStatus.REJECTED) {
            throw new IllegalStateException("Translation is already rejected");
        }
        if (this.status == TranslationStatus.APPROVED) {
            throw new IllegalStateException("Cannot reject an approved translation");
        }
        this.status = TranslationStatus.REJECTED;
    }

    public void updateContent(String title, String content) {
        validateContent(title, content);
        this.title = title;
        this.content = content;
        if (this.status == TranslationStatus.APPROVED) {
            this.status = TranslationStatus.REVIEW_PENDING;
        }
    }

    public static Translation reconstitute(TranslationId id, PostId postId, TenantId tenantId,
            PostLocale locale, String title, String content,
            TranslationStatus status, TranslationSource source,
            Instant translatedAt) {
        return new Translation(id, postId, tenantId, locale, title, content, status, source, translatedAt);
    }

    private static void validateContent(String title, String content) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Translation title must not be blank");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Translation content must not be blank");
        }
    }

    public TranslationId getId() {
        return id;
    }

    public PostId getPostId() {
        return postId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public PostLocale getLocale() {
        return locale;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public TranslationStatus getStatus() {
        return status;
    }

    public TranslationSource getSource() {
        return source;
    }

    public Instant getTranslatedAt() {
        return translatedAt;
    }
}
