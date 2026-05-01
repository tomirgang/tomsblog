package de.tomsblog.blogcontent.adapter.outbound.persistence;

import de.tomsblog.blogcontent.domain.model.*;
import de.tomsblog.shared.domain.AuthorId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class PostMapper {

    private PostMapper() {
    }

    public static PostJpaEntity toEntity(Post post) {
        PostJpaEntity entity = new PostJpaEntity();
        entity.setId(post.getId().value());
        entity.setTenantId(post.getTenantId().value());
        entity.setAuthorId(post.getAuthorId().value());
        entity.setTitle(post.getTitle());
        entity.setSlug(post.getSlug().value());
        entity.setContent(post.getContent());
        entity.setStatus(PostStatusJpa.valueOf(post.getStatus().name()));
        entity.setLocale(post.getLocale().languageTag());
        entity.setTagIds(post.getTags().stream().map(TagId::value).collect(Collectors.toSet()));
        entity.setSources(post.getSources().stream()
                .map(s -> new SourceEmbeddable(s.url(), s.title()))
                .toList());
        entity.setAttachments(post.getAttachments().stream()
                .map(a -> new AttachmentEmbeddable(a.id().value(), a.filename(), a.contentType(), a.size(),
                        a.show(), a.storageKey()))
                .toList());
        entity.setPublishedAt(post.getPublishedAt());
        entity.setSocialMediaTitle(post.getSocialMediaTitle());
        entity.setSocialMediaSummary(post.getSocialMediaSummary());
        return entity;
    }

    public static Post toDomain(PostJpaEntity entity) {
        Set<TagId> tags = entity.getTagIds().stream().map(TagId::of).collect(Collectors.toSet());
        List<Source> sources = entity.getSources().stream()
                .map(s -> new Source(s.getUrl(), s.getTitle()))
                .toList();
        List<Attachment> attachments = entity.getAttachments().stream()
                .map(a -> new Attachment(AttachmentId.of(a.getId()), a.getFilename(), a.getContentType(), a.getSize(),
                        a.isShow(), a.getStorageKey()))
                .toList();
        return Post.reconstitute(
                PostId.of(entity.getId()),
                TenantId.of(entity.getTenantId()),
                AuthorId.of(entity.getAuthorId()),
                entity.getTitle(),
                new Slug(entity.getSlug()),
                entity.getContent(),
                PostStatus.valueOf(entity.getStatus().name()),
                PostLocale.of(entity.getLocale()),
                tags,
                sources,
                attachments,
                entity.getPublishedAt(),
                entity.getSocialMediaTitle(),
                entity.getSocialMediaSummary());
    }
}
