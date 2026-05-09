package de.tomsblog.feed.application.port.outbound;

import de.tomsblog.feed.domain.model.FeedEntry;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for feed entry persistence.
 *
 * @req SWR-090
 */
public interface FeedEntryRepository {

    FeedEntry save(FeedEntry feedEntry);

    Optional<FeedEntry> findByTenantIdAndPostId(UUID tenantId, UUID postId);
}
