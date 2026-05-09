package de.tomsblog.feed.adapter.outbound.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaFeedEntryRepository extends JpaRepository<FeedEntryJpaEntity, UUID> {

    Optional<FeedEntryJpaEntity> findByTenantIdAndPostId(UUID tenantId, UUID postId);
}
