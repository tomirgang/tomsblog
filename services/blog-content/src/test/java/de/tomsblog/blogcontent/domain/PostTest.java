package de.tomsblog.blogcontent.domain;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.blogcontent.domain.event.PostCreatedEvent;
import de.tomsblog.blogcontent.domain.event.PostPublishedEvent;
import de.tomsblog.blogcontent.domain.model.*;
import de.tomsblog.shared.domain.AuthorId;
import de.tomsblog.shared.tenant.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostTest {

    private final TenantId tenantId = TenantId.generate();
    private final AuthorId authorId = AuthorId.generate();

    @Test
    @DisplayName("SWR-001: Create post generates ID and slug from title")
    void createPostGeneratesIdAndSlug() {
        Post post = Post.create(tenantId, authorId, "My First Post", "Some content", PostLocale.german());

        assertThat(post.getId()).isNotNull();
        assertThat(post.getTenantId()).isEqualTo(tenantId);
        assertThat(post.getAuthorId()).isEqualTo(authorId);
        assertThat(post.getTitle()).isEqualTo("My First Post");
        assertThat(post.getSlug().value()).isEqualTo("my-first-post");
        assertThat(post.getStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(post.getLocale()).isEqualTo(PostLocale.german());
    }

    @Test
    @DisplayName("SWR-001: Create post registers PostCreatedEvent")
    void createPostRegistersEvent() {
        Post post = Post.create(tenantId, authorId, "Test Post", "Content", PostLocale.english());

        assertThat(post.getDomainEvents()).hasSize(1);
        assertThat(post.getDomainEvents().getFirst()).isInstanceOf(PostCreatedEvent.class);
    }

    @Test
    @DisplayName("SWR-002: Publish post changes status and registers event")
    void publishPostChangesStatusAndRegistersEvent() {
        Post post = Post.create(tenantId, authorId, "Test Post", "Content", PostLocale.german());
        post.clearDomainEvents();

        post.publish();

        assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(post.getPublishedAt()).isNotNull();
        assertThat(post.getDomainEvents()).hasSize(1);
        assertThat(post.getDomainEvents().getFirst()).isInstanceOf(PostPublishedEvent.class);
    }

    @Test
    @DisplayName("SWR-002: Publishing already published post throws exception")
    void publishAlreadyPublishedPostThrows() {
        Post post = Post.create(tenantId, authorId, "Test Post", "Content", PostLocale.german());
        post.publish();

        assertThatThrownBy(post::publish)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already published");
    }

    @Test
    @DisplayName("SWR-001: Update content changes title, slug, and content")
    void updateContentChangesFields() {
        Post post = Post.create(tenantId, authorId, "Old Title", "Old content", PostLocale.german());

        post.updateContent("New Title", "New content");

        assertThat(post.getTitle()).isEqualTo("New Title");
        assertThat(post.getSlug().value()).isEqualTo("new-title");
        assertThat(post.getContent()).isEqualTo("New content");
    }

    @Test
    @DisplayName("SWR-001: Create post with blank title throws exception")
    void createPostWithBlankTitleThrows() {
        assertThatThrownBy(() -> Post.create(tenantId, authorId, "", "Content", PostLocale.german()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Tags (by ID) can be added and removed")
    void tagsCanBeAddedAndRemoved() {
        Post post = Post.create(tenantId, authorId, "Test", "Content", PostLocale.german());
        TagId tagId = TagId.generate();

        post.addTag(tagId);
        assertThat(post.getTags()).contains(tagId);

        post.removeTag(tagId);
        assertThat(post.getTags()).doesNotContain(tagId);
    }

    @Test
    @DisplayName("SWR-012: Sources can be added and removed")
    void sourcesCanBeAddedAndRemoved() {
        Post post = Post.create(tenantId, authorId, "Test", "Content", PostLocale.german());
        Source source = new Source("https://example.com", "Example");

        post.addSource(source);
        assertThat(post.getSources()).containsExactly(source);

        post.removeSource(source);
        assertThat(post.getSources()).isEmpty();
    }

    @Test
    @DisplayName("Attachments can be added and removed by ID")
    void attachmentsCanBeAddedAndRemoved() {
        Post post = Post.create(tenantId, authorId, "Test", "Content", PostLocale.german());
        Attachment attachment = Attachment.create("photo.jpg", "image/jpeg", 1024, true);

        post.addAttachment(attachment);
        assertThat(post.getAttachments()).containsExactly(attachment);

        post.removeAttachment(attachment.id());
        assertThat(post.getAttachments()).isEmpty();
    }
}
