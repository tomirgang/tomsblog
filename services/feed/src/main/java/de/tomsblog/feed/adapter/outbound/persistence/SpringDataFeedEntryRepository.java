package de.tomsblog.feed.adapter.outbound.persistence;

import de.tomsblog.feed.application.port.outbound.FeedEntryRepository;
import de.tomsblog.feed.domain.model.FeedEntry;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class SpringDataFeedEntryRepository implements FeedEntryRepository {

    private final JpaFeedEntryRepository jpaRepository;

    public SpringDataFeedEntryRepository(JpaFeedEntryRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public FeedEntry save(FeedEntry feedEntry) {
        FeedEntryJpaEntity jpa = FeedEntryMapper.toJpa(feedEntry);
        FeedEntryJpaEntity saved = jpaRepository.save(jpa);
        return FeedEntryMapper.toDomain(saved);
    }

    @Override
    public Optional<FeedEntry> findByTenantIdAndPostId(UUID tenantId, UUID postId) {
        return jpaRepository.findByTenantIdAndPostId(tenantId, postId).map(FeedEntryMapper::toDomain);
    }
}
