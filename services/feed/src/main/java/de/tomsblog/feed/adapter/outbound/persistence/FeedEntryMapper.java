package de.tomsblog.feed.adapter.outbound.persistence;

import de.tomsblog.feed.domain.model.FeedEntry;

final class FeedEntryMapper {

    private FeedEntryMapper() {}

    static FeedEntryJpaEntity toJpa(FeedEntry domain) {
        return new FeedEntryJpaEntity(
                domain.getId(),
                domain.getTenantId(),
                domain.getPostId(),
                domain.getTitle(),
                domain.getSlug(),
                domain.getLocale(),
                domain.getPublishedAt(),
                domain.getUpdatedAt());
    }

    static FeedEntry toDomain(FeedEntryJpaEntity jpa) {
        return FeedEntry.reconstitute(
                jpa.getId(),
                jpa.getTenantId(),
                jpa.getPostId(),
                jpa.getTitle(),
                jpa.getSlug(),
                jpa.getLocale(),
                jpa.getPublishedAt(),
                jpa.getUpdatedAt());
    }
}
