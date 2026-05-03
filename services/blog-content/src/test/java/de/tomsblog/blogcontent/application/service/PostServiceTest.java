package de.tomsblog.blogcontent.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.tomsblog.blogcontent.application.port.inbound.AddSourceCommand;
import de.tomsblog.blogcontent.application.port.inbound.CreatePostCommand;
import de.tomsblog.blogcontent.application.port.inbound.RemoveSourceCommand;
import de.tomsblog.blogcontent.application.port.inbound.UpdatePostCommand;
import de.tomsblog.blogcontent.application.port.outbound.EventPublisher;
import de.tomsblog.blogcontent.application.port.outbound.PostRepository;
import de.tomsblog.blogcontent.domain.event.PostCreatedEvent;
import de.tomsblog.blogcontent.domain.event.PostPublishedEvent;
import de.tomsblog.blogcontent.domain.model.*;
import de.tomsblog.shared.domain.AuthorId;
import de.tomsblog.shared.domain.DomainEvent;
import de.tomsblog.shared.tenant.TenantId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private EventPublisher eventPublisher;

    private PostService postService;

    private final TenantId tenantId = TenantId.generate();
    private final AuthorId authorId = AuthorId.generate();

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, eventPublisher);
    }

    @Test
    @DisplayName("SWR-001: createPost saves and publishes domain event")
    void createPost_savesAndPublishesEvent() {
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        List<DomainEvent> captured = new ArrayList<>();
        doAnswer(inv -> {
                    captured.addAll(inv.getArgument(0));
                    return null;
                })
                .when(eventPublisher)
                .publish(any());

        CreatePostCommand command = new CreatePostCommand(
                tenantId, authorId, "Test Title", "Test Content", "HTML", "de", null, null, null, null);

        Post result = postService.createPost(command);

        assertThat(result.getTitle()).isEqualTo("Test Title");
        assertThat(result.getContent()).isEqualTo("Test Content");
        verify(postRepository).save(any(Post.class));
        assertThat(captured).hasSize(1);
        assertThat(captured.get(0)).isInstanceOf(PostCreatedEvent.class);
    }

    @Test
    @DisplayName("SWR-001: createPost uses German locale as default")
    void createPost_defaultLocale() {
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreatePostCommand command =
                new CreatePostCommand(tenantId, authorId, "Test", "Content", null, null, null, null, null, null);

        Post result = postService.createPost(command);

        assertThat(result.getLocale()).isEqualTo(PostLocale.german());
    }

    @Test
    @DisplayName("SWR-001: updatePost changes title and content")
    void updatePost_changesFields() {
        Post existing = Post.create(tenantId, authorId, "Original", "Original content", PostLocale.german());
        existing.clearDomainEvents();
        when(postRepository.findByIdAndTenantId(existing.getId(), tenantId)).thenReturn(Optional.of(existing));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePostCommand command = new UpdatePostCommand(
                existing.getId(), tenantId, "Updated Title", "Updated content", "SM Title", "SM Summary", null, null);

        Post result = postService.updatePost(command);

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getContent()).isEqualTo("Updated content");
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("SWR-002: publishPost changes status and publishes event")
    void publishPost_changesStatusAndPublishesEvent() {
        Post post = Post.create(tenantId, authorId, "Title", "Content", PostLocale.german());
        post.clearDomainEvents();
        when(postRepository.findByIdAndTenantId(post.getId(), tenantId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        List<DomainEvent> captured = new ArrayList<>();
        doAnswer(inv -> {
                    captured.addAll(inv.getArgument(0));
                    return null;
                })
                .when(eventPublisher)
                .publish(any());

        postService.publishPost(post.getId(), tenantId);

        assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        verify(postRepository).save(post);
        assertThat(captured).hasSize(1);
        assertThat(captured.get(0)).isInstanceOf(PostPublishedEvent.class);
    }

    @Test
    @DisplayName("SWR-001: getPost throws PostNotFoundException when not found")
    void getPost_throwsWhenNotFound() {
        PostId postId = PostId.generate();
        when(postRepository.findByIdAndTenantId(postId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost(postId, tenantId))
                .isInstanceOf(PostNotFoundException.class)
                .satisfies(ex ->
                        assertThat(((PostNotFoundException) ex).getPostId()).isEqualTo(postId));
    }

    @Test
    @DisplayName("SWR-009: listPosts returns all posts for tenant")
    void listPosts_returnsAllForTenant() {
        Post post1 = Post.create(tenantId, authorId, "Post 1", "Content 1", PostLocale.german());
        Post post2 = Post.create(tenantId, authorId, "Post 2", "Content 2", PostLocale.german());
        when(postRepository.findAllByTenantId(tenantId)).thenReturn(List.of(post1, post2));

        List<Post> result = postService.listPosts(tenantId);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("SWR-001: deletePost delegates to repository")
    void deletePost_delegatesToRepository() {
        PostId postId = PostId.generate();

        postService.deletePost(postId, tenantId);

        verify(postRepository).deleteByIdAndTenantId(postId, tenantId);
    }

    @Test
    @DisplayName("SWR-012: addSource adds source to post and saves")
    void addSource_addsAndSaves() {
        Post post = Post.create(tenantId, authorId, "Post", "Content", PostLocale.german());
        post.clearDomainEvents();
        when(postRepository.findByIdAndTenantId(post.getId(), tenantId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddSourceCommand command = new AddSourceCommand(post.getId(), tenantId, "https://example.com", "Example");

        Post result = postService.addSource(command);

        assertThat(result.getSources()).hasSize(1);
        assertThat(result.getSources().get(0).url()).isEqualTo("https://example.com");
        assertThat(result.getSources().get(0).title()).isEqualTo("Example");
        verify(postRepository).save(post);
    }

    @Test
    @DisplayName("SWR-012: addSource throws when post not found")
    void addSource_throwsWhenPostNotFound() {
        PostId postId = PostId.generate();
        when(postRepository.findByIdAndTenantId(postId, tenantId)).thenReturn(Optional.empty());

        AddSourceCommand command = new AddSourceCommand(postId, tenantId, "https://example.com", "Example");

        assertThatThrownBy(() -> postService.addSource(command)).isInstanceOf(PostNotFoundException.class);
    }

    @Test
    @DisplayName("SWR-012: removeSource removes source from post and saves")
    void removeSource_removesAndSaves() {
        Post post = Post.create(tenantId, authorId, "Post", "Content", PostLocale.german());
        post.addSource(new Source("https://example.com", "Example"));
        post.clearDomainEvents();
        when(postRepository.findByIdAndTenantId(post.getId(), tenantId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RemoveSourceCommand command = new RemoveSourceCommand(post.getId(), tenantId, "https://example.com", "Example");

        Post result = postService.removeSource(command);

        assertThat(result.getSources()).isEmpty();
        verify(postRepository).save(post);
    }

    @Test
    @DisplayName("SWR-012: removeSource throws when post not found")
    void removeSource_throwsWhenPostNotFound() {
        PostId postId = PostId.generate();
        when(postRepository.findByIdAndTenantId(postId, tenantId)).thenReturn(Optional.empty());

        RemoveSourceCommand command = new RemoveSourceCommand(postId, tenantId, "https://example.com", "Example");

        assertThatThrownBy(() -> postService.removeSource(command)).isInstanceOf(PostNotFoundException.class);
    }

    @Test
    @DisplayName("SWR-012: listSources returns sources of the post")
    void listSources_returnsSources() {
        Post post = Post.create(tenantId, authorId, "Post", "Content", PostLocale.german());
        post.addSource(new Source("https://example.com", "Example"));
        post.addSource(new Source("https://docs.spring.io", "Spring Docs"));
        post.clearDomainEvents();
        when(postRepository.findByIdAndTenantId(post.getId(), tenantId)).thenReturn(Optional.of(post));

        List<Source> sources = postService.listSources(post.getId(), tenantId);

        assertThat(sources).hasSize(2);
    }

    @Test
    @DisplayName("SWR-012: listSources throws when post not found")
    void listSources_throwsWhenPostNotFound() {
        PostId postId = PostId.generate();
        when(postRepository.findByIdAndTenantId(postId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.listSources(postId, tenantId)).isInstanceOf(PostNotFoundException.class);
    }

    @Test
    @DisplayName("SWR-026: listPublishedPosts delegates to repository")
    void listPublishedPosts_delegatesToRepository() {
        Post post1 = Post.create(tenantId, authorId, "Post 1", "Content 1", PostLocale.german());
        post1.publish();
        Post post2 = Post.create(tenantId, authorId, "Post 2", "Content 2", PostLocale.german());
        post2.publish();
        when(postRepository.findPublishedByTenantId(tenantId)).thenReturn(List.of(post1, post2));

        List<Post> result = postService.listPublishedPosts(tenantId);

        assertThat(result).hasSize(2);
        verify(postRepository).findPublishedByTenantId(tenantId);
    }

    @Test
    @DisplayName("SWR-033: listRecentPublishedPosts delegates to repository with limit")
    void listRecentPublishedPosts_delegatesToRepository() {
        Post post1 = Post.create(tenantId, authorId, "Post 1", "Content 1", PostLocale.german());
        post1.publish();
        when(postRepository.findRecentPublishedByTenantId(tenantId, 3)).thenReturn(List.of(post1));

        List<Post> result = postService.listRecentPublishedPosts(tenantId, 3);

        assertThat(result).hasSize(1);
        verify(postRepository).findRecentPublishedByTenantId(tenantId, 3);
    }

    @Test
    @DisplayName("SWR-026: getPublishedPostBySlug returns published post")
    void getPublishedPostBySlug_returnsPublishedPost() {
        Slug slug = new Slug("test-post");
        Post post = Post.create(tenantId, authorId, "Test Post", "Content", PostLocale.german());
        post.publish();
        when(postRepository.findBySlugAndTenantId(slug, tenantId)).thenReturn(Optional.of(post));

        Post result = postService.getPublishedPostBySlug(slug, tenantId);

        assertThat(result.getTitle()).isEqualTo("Test Post");
        assertThat(result.getStatus()).isEqualTo(PostStatus.PUBLISHED);
    }

    @Test
    @DisplayName("SWR-026: getPublishedPostBySlug throws when not found")
    void getPublishedPostBySlug_throwsWhenNotFound() {
        Slug slug = new Slug("nonexistent");
        when(postRepository.findBySlugAndTenantId(slug, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPublishedPostBySlug(slug, tenantId))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    @DisplayName("SWR-026: getPublishedPostBySlug throws when post is draft")
    void getPublishedPostBySlug_throwsWhenDraft() {
        Slug slug = new Slug("draft-post");
        Post post = Post.create(tenantId, authorId, "Draft Post", "Content", PostLocale.german());
        when(postRepository.findBySlugAndTenantId(slug, tenantId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.getPublishedPostBySlug(slug, tenantId))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    @DisplayName("SWR-038: searchPublishedPosts delegates to repository when query is present")
    void searchPublishedPosts_withQuery_delegatesToRepository() {
        Post post = Post.create(tenantId, authorId, "Found Post", "Content", PostLocale.german());
        post.publish();
        when(postRepository.searchPublished("test", tenantId)).thenReturn(List.of(post));

        List<Post> result = postService.searchPublishedPosts("test", tenantId);

        assertThat(result).hasSize(1);
        verify(postRepository).searchPublished("test", tenantId);
    }

    @Test
    @DisplayName("SWR-038: searchPublishedPosts returns all published when query is blank")
    void searchPublishedPosts_blankQuery_returnsAllPublished() {
        Post post = Post.create(tenantId, authorId, "Post", "Content", PostLocale.german());
        post.publish();
        when(postRepository.findPublishedByTenantId(tenantId)).thenReturn(List.of(post));

        List<Post> result = postService.searchPublishedPosts("  ", tenantId);

        assertThat(result).hasSize(1);
        verify(postRepository).findPublishedByTenantId(tenantId);
        verify(postRepository, never()).searchPublished(any(), any());
    }

    @Test
    @DisplayName("SWR-038: searchPublishedPosts returns all published when query is null")
    void searchPublishedPosts_nullQuery_returnsAllPublished() {
        when(postRepository.findPublishedByTenantId(tenantId)).thenReturn(List.of());

        List<Post> result = postService.searchPublishedPosts(null, tenantId);

        assertThat(result).isEmpty();
        verify(postRepository).findPublishedByTenantId(tenantId);
    }

    @Test
    @DisplayName("SWR-039: findPreviousPublishedPost returns previous post")
    void findPreviousPublishedPost_returnsPrevious() {
        Slug slug = new Slug("current-post");
        Post current = Post.create(tenantId, authorId, "Current Post", "Content", PostLocale.german());
        current.publish();
        Post previous = Post.create(tenantId, authorId, "Previous Post", "Content", PostLocale.german());
        previous.publish();
        when(postRepository.findBySlugAndTenantId(slug, tenantId)).thenReturn(Optional.of(current));
        when(postRepository.findPreviousPublished(tenantId, current.getPublishedAt()))
                .thenReturn(Optional.of(previous));

        Optional<Post> result = postService.findPreviousPublishedPost(slug, tenantId);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Previous Post");
    }

    @Test
    @DisplayName("SWR-039: findPreviousPublishedPost returns empty when post has no publishedAt")
    void findPreviousPublishedPost_unpublished_returnsEmpty() {
        Slug slug = new Slug("draft-post");
        Post draft = Post.create(tenantId, authorId, "Draft Post", "Content", PostLocale.german());
        when(postRepository.findBySlugAndTenantId(slug, tenantId)).thenReturn(Optional.of(draft));

        Optional<Post> result = postService.findPreviousPublishedPost(slug, tenantId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("SWR-039: findPreviousPublishedPost throws when slug not found")
    void findPreviousPublishedPost_throwsWhenNotFound() {
        Slug slug = new Slug("missing");
        when(postRepository.findBySlugAndTenantId(slug, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.findPreviousPublishedPost(slug, tenantId))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    @DisplayName("SWR-039: findNextPublishedPost returns next post")
    void findNextPublishedPost_returnsNext() {
        Slug slug = new Slug("current-post");
        Post current = Post.create(tenantId, authorId, "Current Post", "Content", PostLocale.german());
        current.publish();
        Post next = Post.create(tenantId, authorId, "Next Post", "Content", PostLocale.german());
        next.publish();
        when(postRepository.findBySlugAndTenantId(slug, tenantId)).thenReturn(Optional.of(current));
        when(postRepository.findNextPublished(tenantId, current.getPublishedAt()))
                .thenReturn(Optional.of(next));

        Optional<Post> result = postService.findNextPublishedPost(slug, tenantId);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Next Post");
    }

    @Test
    @DisplayName("SWR-039: findNextPublishedPost returns empty when post has no publishedAt")
    void findNextPublishedPost_unpublished_returnsEmpty() {
        Slug slug = new Slug("draft-post");
        Post draft = Post.create(tenantId, authorId, "Draft Post", "Content", PostLocale.german());
        when(postRepository.findBySlugAndTenantId(slug, tenantId)).thenReturn(Optional.of(draft));

        Optional<Post> result = postService.findNextPublishedPost(slug, tenantId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("SWR-040: getPostIfPublished returns published post")
    void getPostIfPublished_returnsPublishedPost() {
        Post post = Post.create(tenantId, authorId, "Published", "Content", PostLocale.german());
        post.publish();
        when(postRepository.findByIdAndTenantId(post.getId(), tenantId)).thenReturn(Optional.of(post));

        Optional<Post> result = postService.getPostIfPublished(post.getId(), tenantId);

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("SWR-040: getPostIfPublished returns empty for draft post")
    void getPostIfPublished_returnsEmptyForDraft() {
        Post post = Post.create(tenantId, authorId, "Draft", "Content", PostLocale.german());
        when(postRepository.findByIdAndTenantId(post.getId(), tenantId)).thenReturn(Optional.of(post));

        Optional<Post> result = postService.getPostIfPublished(post.getId(), tenantId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("SWR-040: getPostIfPublished returns empty for nonexistent post")
    void getPostIfPublished_returnsEmptyForMissing() {
        PostId postId = PostId.generate();
        when(postRepository.findByIdAndTenantId(postId, tenantId)).thenReturn(Optional.empty());

        Optional<Post> result = postService.getPostIfPublished(postId, tenantId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("SWR-040: createPost passes series navigation fields")
    void createPost_withSeriesNavigation() {
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UUID prevId = UUID.randomUUID();
        UUID nextId = UUID.randomUUID();

        CreatePostCommand command = new CreatePostCommand(
                tenantId, authorId, "Series Post", "Content", "HTML", "de", null, null, prevId, nextId);

        Post result = postService.createPost(command);

        assertThat(result.getSeriesPreviousPostId()).isNotNull();
        assertThat(result.getSeriesPreviousPostId().value()).isEqualTo(prevId);
        assertThat(result.getSeriesNextPostId()).isNotNull();
        assertThat(result.getSeriesNextPostId().value()).isEqualTo(nextId);
    }

    @Test
    @DisplayName("SWR-040: updatePost passes series navigation fields")
    void updatePost_withSeriesNavigation() {
        Post existing = Post.create(tenantId, authorId, "Original", "Content", PostLocale.german());
        existing.clearDomainEvents();
        when(postRepository.findByIdAndTenantId(existing.getId(), tenantId)).thenReturn(Optional.of(existing));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UUID prevId = UUID.randomUUID();

        UpdatePostCommand command =
                new UpdatePostCommand(existing.getId(), tenantId, "Updated", "Content", null, null, prevId, null);

        Post result = postService.updatePost(command);

        assertThat(result.getSeriesPreviousPostId()).isNotNull();
        assertThat(result.getSeriesPreviousPostId().value()).isEqualTo(prevId);
        assertThat(result.getSeriesNextPostId()).isNull();
    }
}
