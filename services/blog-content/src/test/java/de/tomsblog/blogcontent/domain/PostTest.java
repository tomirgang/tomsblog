package de.tomsblog.blogcontent.domain;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.blogcontent.domain.event.PostCreatedEvent;
import de.tomsblog.blogcontent.domain.event.PostPublishedEvent;
import de.tomsblog.blogcontent.domain.model.*;
import de.tomsblog.shared.tenant.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostTest {

    private final TenantId tenantId = TenantId.generate();

    @Test
    @DisplayName("SWR-001: Create post generates ID and slug from title")
    void createPostGeneratesIdAndSlug() {
        Post post = Post.create(tenantId, "My First Post", "Some content", PostLocale.german());

        assertThat(post.getId()).isNotNull();
        assertThat(post.getTenantId()).isEqualTo(tenantId);
        assertThat(post.getTitle()).isEqualTo("My First Post");
        assertThat(post.getSlug().value()).isEqualTo("my-first-post");
        assertThat(post.getStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(post.getLocale()).isEqualTo(PostLocale.german());
    }

    @Test
    @DisplayName("SWR-001: Create post registers PostCreatedEvent")
    void createPostRegistersEvent() {
        Post post = Post.create(tenantId, "Test Post", "Content", PostLocale.english());

        assertThat(post.getDomainEvents()).hasSize(1);
        assertThat(post.getDomainEvents().getFirst()).isInstanceOf(PostCreatedEvent.class);
    }

    @Test
    @DisplayName("SWR-002: Publish post changes status and registers event")
    void publishPostChangesStatusAndRegistersEvent() {
        Post post = Post.create(tenantId, "Test Post", "Content", PostLocale.german());
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
        Post post = Post.create(tenantId, "Test Post", "Content", PostLocale.german());
        post.publish();

        assertThatThrownBy(post::publish)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already published");
    }

    @Test
    @DisplayName("SWR-001: Update content changes title, slug, and content")
    void updateContentChangesFields() {
        Post post = Post.create(tenantId, "Old Title", "Old content", PostLocale.german());

        post.updateContent("New Title", "New content");

        assertThat(post.getTitle()).isEqualTo("New Title");
        assertThat(post.getSlug().value()).isEqualTo("new-title");
        assertThat(post.getContent()).isEqualTo("New content");
    }

    @Test
    @DisplayName("SWR-001: Create post with blank title throws exception")
    void createPostWithBlankTitleThrows() {
        assertThatThrownBy(() -> Post.create(tenantId, "", "Content", PostLocale.german()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Tags can be added and removed")
    void tagsCanBeAddedAndRemoved() {
        Post post = Post.create(tenantId, "Test", "Content", PostLocale.german());
        Tag java = new Tag("java");

        post.addTag(java);
        assertThat(post.getTags()).contains(java);

        post.removeTag(java);
        assertThat(post.getTags()).doesNotContain(java);
    }
}
