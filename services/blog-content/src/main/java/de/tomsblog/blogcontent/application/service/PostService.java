package de.tomsblog.blogcontent.application.service;

import de.tomsblog.blogcontent.application.port.inbound.AddSourceCommand;
import de.tomsblog.blogcontent.application.port.inbound.CreatePostCommand;
import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.application.port.inbound.RemoveSourceCommand;
import de.tomsblog.blogcontent.application.port.inbound.UpdatePostCommand;
import de.tomsblog.blogcontent.application.port.outbound.EventPublisher;
import de.tomsblog.blogcontent.application.port.outbound.PostRepository;
import de.tomsblog.blogcontent.domain.model.Post;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.PostLocale;
import de.tomsblog.blogcontent.domain.model.PostStatus;
import de.tomsblog.blogcontent.domain.model.Slug;
import de.tomsblog.blogcontent.domain.model.Source;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;

/**
 * Application service orchestrating post use cases.
 *
 * @req SWR-001
 * @req SWR-002
 * @req SWR-009
 * @req SWR-026
 */
public class PostService implements PostUseCase {

    private final PostRepository postRepository;
    private final EventPublisher eventPublisher;

    public PostService(PostRepository postRepository, EventPublisher eventPublisher) {
        this.postRepository = postRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Post createPost(CreatePostCommand command) {
        PostLocale locale = command.locale() != null ? PostLocale.of(command.locale()) : PostLocale.german();
        Post post = Post.create(command.tenantId(), command.authorId(), command.title(), command.content(), locale);
        post.updateSocialMedia(command.socialMediaTitle(), command.socialMediaSummary());
        Post saved = postRepository.save(post);
        eventPublisher.publish(post.getDomainEvents());
        post.clearDomainEvents();
        return saved;
    }

    @Override
    public Post updatePost(UpdatePostCommand command) {
        Post post = findOrThrow(command.postId(), command.tenantId());
        post.updateContent(command.title(), command.content());
        post.updateSocialMedia(command.socialMediaTitle(), command.socialMediaSummary());
        return postRepository.save(post);
    }

    @Override
    public void publishPost(PostId postId, TenantId tenantId) {
        Post post = findOrThrow(postId, tenantId);
        post.publish();
        postRepository.save(post);
        eventPublisher.publish(post.getDomainEvents());
        post.clearDomainEvents();
    }

    @Override
    public void deletePost(PostId postId, TenantId tenantId) {
        postRepository.deleteByIdAndTenantId(postId, tenantId);
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
        return postRepository.save(post);
    }

    /** @req SWR-012 */
    @Override
    public Post removeSource(RemoveSourceCommand command) {
        Post post = findOrThrow(command.postId(), command.tenantId());
        Source source = new Source(command.url(), command.title());
        post.removeSource(source);
        return postRepository.save(post);
    }

    /** @req SWR-012 */
    @Override
    public List<Source> listSources(PostId postId, TenantId tenantId) {
        Post post = findOrThrow(postId, tenantId);
        return post.getSources();
    }

    private Post findOrThrow(PostId postId, TenantId tenantId) {
        return postRepository
                .findByIdAndTenantId(postId, tenantId)
                .orElseThrow(() -> new PostNotFoundException(postId));
    }
}
