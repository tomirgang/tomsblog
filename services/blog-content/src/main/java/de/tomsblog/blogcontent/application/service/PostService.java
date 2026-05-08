package de.tomsblog.blogcontent.application.service;

import de.tomsblog.blogcontent.application.port.inbound.AddSourceCommand;
import de.tomsblog.blogcontent.application.port.inbound.CreatePostCommand;
import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.application.port.inbound.RemoveSourceCommand;
import de.tomsblog.blogcontent.application.port.inbound.UpdatePostCommand;
import de.tomsblog.blogcontent.application.port.outbound.EventPublisher;
import de.tomsblog.blogcontent.application.port.outbound.PostRepository;
import de.tomsblog.blogcontent.domain.model.ContentType;
import de.tomsblog.blogcontent.domain.model.Post;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.PostLocale;
import de.tomsblog.blogcontent.domain.model.PostStatus;
import de.tomsblog.blogcontent.domain.model.Slug;
import de.tomsblog.blogcontent.domain.model.Source;
import de.tomsblog.blogcontent.domain.model.TagId;
import de.tomsblog.shared.audit.AuditLogEntry;
import de.tomsblog.shared.audit.AuditLogger;
import de.tomsblog.shared.tenant.TenantId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Application service orchestrating post use cases.
 *
 * @req SWR-001
 * @req SWR-002
 * @req SWR-009
 * @req SWR-026
 * @req SWR-038
 * @req SWR-039
 * @req SWR-040
 * @req SWR-042
 * @req SWR-056
 */
public class PostService implements PostUseCase {

    private final PostRepository postRepository;
    private final EventPublisher eventPublisher;
    private final AuditLogger auditLogger;

    public PostService(PostRepository postRepository, EventPublisher eventPublisher, AuditLogger auditLogger) {
        this.postRepository = postRepository;
        this.eventPublisher = eventPublisher;
        this.auditLogger = auditLogger;
    }

    @Override
    public Post createPost(CreatePostCommand command) {
        PostLocale locale = command.locale() != null ? PostLocale.of(command.locale()) : PostLocale.german();
        ContentType contentType =
                command.contentType() != null ? ContentType.valueOf(command.contentType()) : ContentType.HTML;
        Post post = Post.create(
                command.tenantId(), command.authorId(), command.title(), command.content(), contentType, locale);
        post.updateSocialMedia(command.socialMediaTitle(), command.socialMediaSummary());
        post.updateSeriesNavigation(
                command.seriesPreviousPostId() != null ? PostId.of(command.seriesPreviousPostId()) : null,
                command.seriesNextPostId() != null ? PostId.of(command.seriesNextPostId()) : null);
        post.updateFeatured(command.featuredFrom(), command.featuredUntil());
        Post saved = postRepository.save(post);
        eventPublisher.publish(post.getDomainEvents());
        post.clearDomainEvents();
        auditLogger.log(AuditLogEntry.create(
                saved.getTenantId().toString(),
                command.authorId().toString(),
                "POST_CREATED",
                "Post",
                saved.getId().asString()));
        return saved;
    }

    @Override
    public Post updatePost(UpdatePostCommand command) {
        Post post = findOrThrow(command.postId(), command.tenantId());
        post.updateContent(command.title(), command.content(), ContentType.valueOf(command.contentType()));
        post.updateSocialMedia(command.socialMediaTitle(), command.socialMediaSummary());
        post.updateSeriesNavigation(
                command.seriesPreviousPostId() != null ? PostId.of(command.seriesPreviousPostId()) : null,
                command.seriesNextPostId() != null ? PostId.of(command.seriesNextPostId()) : null);
        post.updateFeatured(command.featuredFrom(), command.featuredUntil());
        Post saved = postRepository.save(post);
        auditLogger.log(AuditLogEntry.create(
                command.tenantId().toString(),
                post.getAuthorId().toString(),
                "POST_UPDATED",
                "Post",
                command.postId().asString()));
        return saved;
    }

    @Override
    public void publishPost(PostId postId, TenantId tenantId) {
        Post post = findOrThrow(postId, tenantId);
        post.publish();
        postRepository.save(post);
        eventPublisher.publish(post.getDomainEvents());
        post.clearDomainEvents();
        auditLogger.log(AuditLogEntry.create(
                tenantId.toString(), post.getAuthorId().toString(), "POST_PUBLISHED", "Post", postId.asString()));
    }

    @Override
    public void deletePost(PostId postId, TenantId tenantId) {
        Post post = findOrThrow(postId, tenantId);
        postRepository.deleteByIdAndTenantId(postId, tenantId);
        auditLogger.log(AuditLogEntry.create(
                tenantId.toString(), post.getAuthorId().toString(), "POST_DELETED", "Post", postId.asString()));
    }

    @Override
    public Post getPost(PostId postId, TenantId tenantId) {
        return findOrThrow(postId, tenantId);
    }

    @Override
    public List<Post> listPosts(TenantId tenantId) {
        return postRepository.findAllByTenantId(tenantId);
    }

    /** @req SWR-026 */
    @Override
    public List<Post> listPublishedPosts(TenantId tenantId) {
        return postRepository.findPublishedByTenantId(tenantId);
    }

    /** @req SWR-033 */
    @Override
    public List<Post> listRecentPublishedPosts(TenantId tenantId, int limit) {
        return postRepository.findRecentPublishedByTenantId(tenantId, limit);
    }

    /** @req SWR-026 */
    @Override
    public Post getPublishedPostBySlug(Slug slug, TenantId tenantId) {
        Post post =
                postRepository.findBySlugAndTenantId(slug, tenantId).orElseThrow(() -> new PostNotFoundException(slug));
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new PostNotFoundException(slug);
        }
        return post;
    }

    /** @req SWR-012 */
    @Override
    public Post addSource(AddSourceCommand command) {
        Post post = findOrThrow(command.postId(), command.tenantId());
        Source source = new Source(command.url(), command.title());
        post.addSource(source);
        Post saved = postRepository.save(post);
        auditLogger.log(AuditLogEntry.create(
                command.tenantId().toString(),
                post.getAuthorId().toString(),
                "SOURCE_ADDED",
                "Post",
                command.postId().asString(),
                command.url()));
        return saved;
    }

    /** @req SWR-012 */
    @Override
    public Post removeSource(RemoveSourceCommand command) {
        Post post = findOrThrow(command.postId(), command.tenantId());
        Source source = new Source(command.url(), command.title());
        post.removeSource(source);
        Post saved = postRepository.save(post);
        auditLogger.log(AuditLogEntry.create(
                command.tenantId().toString(),
                post.getAuthorId().toString(),
                "SOURCE_REMOVED",
                "Post",
                command.postId().asString(),
                command.url()));
        return saved;
    }

    /** @req SWR-012 */
    @Override
    public List<Source> listSources(PostId postId, TenantId tenantId) {
        Post post = findOrThrow(postId, tenantId);
        return post.getSources();
    }

    /** @req SWR-038 */
    @Override
    public List<Post> searchPublishedPosts(String query, TenantId tenantId) {
        if (query == null || query.isBlank()) {
            return postRepository.findPublishedByTenantId(tenantId);
        }
        return postRepository.searchPublished(query, tenantId);
    }

    /** @req SWR-039 */
    @Override
    public Optional<Post> findPreviousPublishedPost(Slug slug, TenantId tenantId) {
        Post current =
                postRepository.findBySlugAndTenantId(slug, tenantId).orElseThrow(() -> new PostNotFoundException(slug));
        if (current.getPublishedAt() == null) {
            return Optional.empty();
        }
        return postRepository.findPreviousPublished(tenantId, current.getPublishedAt());
    }

    /** @req SWR-039 */
    @Override
    public Optional<Post> findNextPublishedPost(Slug slug, TenantId tenantId) {
        Post current =
                postRepository.findBySlugAndTenantId(slug, tenantId).orElseThrow(() -> new PostNotFoundException(slug));
        if (current.getPublishedAt() == null) {
            return Optional.empty();
        }
        return postRepository.findNextPublished(tenantId, current.getPublishedAt());
    }

    /** @req SWR-040 */
    @Override
    public Optional<Post> getPostIfPublished(PostId postId, TenantId tenantId) {
        return postRepository
                .findByIdAndTenantId(postId, tenantId)
                .filter(post -> post.getStatus() == PostStatus.PUBLISHED);
    }

    /** @req SWR-042 */
    @Override
    public List<Post> listFeaturedPosts(TenantId tenantId, LocalDate today) {
        return postRepository.findFeaturedByTenantId(tenantId, today);
    }

    /** @req SWR-085 */
    @Override
    public Post syncPostTags(PostId postId, TenantId tenantId, Set<TagId> tagIds) {
        Post post = findOrThrow(postId, tenantId);
        Set<TagId> currentTags = Set.copyOf(post.getTags());
        for (TagId tagId : currentTags) {
            if (!tagIds.contains(tagId)) {
                post.removeTag(tagId);
            }
        }
        for (TagId tagId : tagIds) {
            if (!currentTags.contains(tagId)) {
                post.addTag(tagId);
            }
        }
        Post saved = postRepository.save(post);
        auditLogger.log(AuditLogEntry.create(
                tenantId.toString(), post.getAuthorId().toString(), "POST_TAGS_SYNCED", "Post", postId.asString()));
        return saved;
    }

    private Post findOrThrow(PostId postId, TenantId tenantId) {
        return postRepository
                .findByIdAndTenantId(postId, tenantId)
                .orElseThrow(() -> new PostNotFoundException(postId));
    }
}
