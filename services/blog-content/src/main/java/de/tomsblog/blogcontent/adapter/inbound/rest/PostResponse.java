package de.tomsblog.blogcontent.adapter.inbound.rest;

import de.tomsblog.blogcontent.domain.model.Attachment;
import de.tomsblog.blogcontent.domain.model.Post;
import de.tomsblog.blogcontent.domain.model.Source;
import de.tomsblog.blogcontent.domain.model.TagId;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record PostResponse(
        UUID id,
        UUID tenantId,
        UUID authorId,
        String title,
        String slug,
        String content,
        String status,
        String locale,
        Set<UUID> tags,
        List<SourceResponse> sources,
        List<AttachmentResponse> attachments,
        Instant publishedAt) {

    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId().value(),
                post.getTenantId().value(),
                post.getAuthorId().value(),
                post.getTitle(),
                post.getSlug().value(),
                post.getContent(),
                post.getStatus().name(),
                post.getLocale().languageTag(),
                post.getTags().stream().map(TagId::value).collect(Collectors.toSet()),
                post.getSources().stream().map(SourceResponse::from).toList(),
                post.getAttachments().stream().map(AttachmentResponse::from).toList(),
                post.getPublishedAt());
    }

    public record SourceResponse(String url, String title) {
        public static SourceResponse from(Source source) {
            return new SourceResponse(source.url(), source.title());
        }
    }

    public record AttachmentResponse(UUID id, String filename, String contentType, long size, boolean show) {
        public static AttachmentResponse from(Attachment attachment) {
            return new AttachmentResponse(
                    attachment.id().value(), attachment.filename(),
                    attachment.contentType(), attachment.size(), attachment.show());
        }
    }
}
