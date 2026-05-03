package de.tomsblog.blogcontent.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import de.tomsblog.blogcontent.domain.model.*;
import de.tomsblog.shared.domain.AuthorId;
import de.tomsblog.shared.tenant.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostMapperTest {

    @Test
    @DisplayName("SWR-040: toEntity maps non-null series fields")
    void toEntity_mapsNonNullSeriesFields() {
        UUID prevId = UUID.randomUUID();
        UUID nextId = UUID.randomUUID();
        Post post = Post.reconstitute(
                PostId.generate(),
                TenantId.generate(),
                AuthorId.generate(),
                "Title",
                Slug.fromTitle("Title"),
                "Content",
                ContentType.HTML,
                PostStatus.DRAFT,
                PostLocale.german(),
                Set.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                PostId.of(prevId),
                PostId.of(nextId));

        PostJpaEntity entity = PostMapper.toEntity(post);

        assertThat(entity.getSeriesPreviousPostId()).isEqualTo(prevId);
        assertThat(entity.getSeriesNextPostId()).isEqualTo(nextId);
    }

    @Test
    @DisplayName("SWR-040: toEntity maps null series fields")
    void toEntity_mapsNullSeriesFields() {
        Post post = Post.create(TenantId.generate(), AuthorId.generate(), "Title", "Content", PostLocale.german());

        PostJpaEntity entity = PostMapper.toEntity(post);

        assertThat(entity.getSeriesPreviousPostId()).isNull();
        assertThat(entity.getSeriesNextPostId()).isNull();
    }

    @Test
    @DisplayName("SWR-040: toDomain maps non-null series fields")
    void toDomain_mapsNonNullSeriesFields() {
        UUID prevId = UUID.randomUUID();
        UUID nextId = UUID.randomUUID();
        PostJpaEntity entity = new PostJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(UUID.randomUUID());
        entity.setAuthorId(UUID.randomUUID());
        entity.setTitle("Title");
        entity.setSlug("title");
        entity.setContent("Content");
        entity.setContentType("HTML");
        entity.setStatus(PostStatusJpa.DRAFT);
        entity.setLocale("de");
        entity.setTagIds(Set.of());
        entity.setSources(List.of());
        entity.setAttachments(List.of());
        entity.setPublishedAt(Instant.now());
        entity.setSeriesPreviousPostId(prevId);
        entity.setSeriesNextPostId(nextId);

        Post post = PostMapper.toDomain(entity);

        assertThat(post.getSeriesPreviousPostId()).isNotNull();
        assertThat(post.getSeriesPreviousPostId().value()).isEqualTo(prevId);
        assertThat(post.getSeriesNextPostId()).isNotNull();
        assertThat(post.getSeriesNextPostId().value()).isEqualTo(nextId);
    }

    @Test
    @DisplayName("SWR-040: toDomain maps null series fields")
    void toDomain_mapsNullSeriesFields() {
        PostJpaEntity entity = new PostJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(UUID.randomUUID());
        entity.setAuthorId(UUID.randomUUID());
        entity.setTitle("Title");
        entity.setSlug("title");
        entity.setContent("Content");
        entity.setContentType("HTML");
        entity.setStatus(PostStatusJpa.DRAFT);
        entity.setLocale("de");
        entity.setTagIds(Set.of());
        entity.setSources(List.of());
        entity.setAttachments(List.of());
        entity.setSeriesPreviousPostId(null);
        entity.setSeriesNextPostId(null);

        Post post = PostMapper.toDomain(entity);

        assertThat(post.getSeriesPreviousPostId()).isNull();
        assertThat(post.getSeriesNextPostId()).isNull();
    }
}
