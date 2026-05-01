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

        CreatePostCommand command =
                new CreatePostCommand(tenantId, authorId, "Test Title", "Test Content", "de", null, null);

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

        CreatePostCommand command = new CreatePostCommand(tenantId, authorId, "Test", "Content", null, null, null);

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
                existing.getId(), tenantId, "Updated Title", "Updated content", "SM Title", "SM Summary");

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
}
