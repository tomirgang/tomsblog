package de.tomsblog.blogcontent.adapter.outbound.persistence;

import de.tomsblog.blogcontent.domain.model.*;
import de.tomsblog.shared.tenant.TenantId;
import java.util.Set;
import java.util.stream.Collectors;

public final class PostMapper {

    private PostMapper() {
    }

    public static PostJpaEntity toEntity(Post post) {
        PostJpaEntity entity = new PostJpaEntity();
        entity.setId(post.getId().value());
        entity.setTenantId(post.getTenantId().value());
        entity.setTitle(post.getTitle());
        entity.setSlug(post.getSlug().value());
        entity.setContent(post.getContent());
        entity.setStatus(PostStatusJpa.valueOf(post.getStatus().name()));
        entity.setLocale(post.getLocale().languageTag());
        entity.setTags(post.getTags().stream().map(Tag::name).collect(Collectors.toSet()));
        entity.setPublishedAt(post.getPublishedAt());
        return entity;
    }

    public static Post toDomain(PostJpaEntity entity) {
        Set<Tag> tags = entity.getTags().stream().map(Tag::new).collect(Collectors.toSet());
        return Post.reconstitute(
                PostId.of(entity.getId()),
                TenantId.of(entity.getTenantId()),
                entity.getTitle(),
                new Slug(entity.getSlug()),
                entity.getContent(),
                PostStatus.valueOf(entity.getStatus().name()),
                PostLocale.of(entity.getLocale()),
                tags,
                entity.getPublishedAt());
    }
}
