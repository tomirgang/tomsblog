package de.tomsblog.feed.application.port.outbound;

import de.tomsblog.feed.domain.model.FeedEntry;
import de.tomsblog.shared.tenant.TenantId;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for feed entry persistence.
 *
 * @req SWR-090
 */
public interface FeedEntryRepository {

    FeedEntry save(FeedEntry feedEntry);

    Optional<FeedEntry> findByTenantIdAndPostId(TenantId tenantId, UUID postId);
}
