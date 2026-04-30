package de.tomsblog.blogcontent.adapter.inbound.rest;

import de.tomsblog.blogcontent.domain.model.Post;
import de.tomsblog.blogcontent.domain.model.Tag;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record PostResponse(
        UUID id,
        UUID tenantId,
        String title,
        String slug,
        String content,
        String status,
        String locale,
        Set<String> tags,
        Instant publishedAt) {

    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId().value(),
                post.getTenantId().value(),
                post.getTitle(),
                post.getSlug().value(),
                post.getContent(),
                post.getStatus().name(),
                post.getLocale().languageTag(),
                post.getTags().stream().map(Tag::name).collect(Collectors.toSet()),
                post.getPublishedAt());
    }
}
