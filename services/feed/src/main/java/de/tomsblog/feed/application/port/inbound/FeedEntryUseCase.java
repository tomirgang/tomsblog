package de.tomsblog.feed.application.port.inbound;

import java.time.Instant;
import java.util.UUID;

/**
 * Inbound port for processing feed-related events.
 *
 * @req SWR-090
 */
public interface FeedEntryUseCase {

    /**
     * Handles a post-published event by creating or updating a feed entry.
     */
    void onPostPublished(UUID tenantId, UUID postId, String slug, String locale, Instant publishedAt);

    /**
     * Handles a post-updated event by updating an existing feed entry.
     */
    void onPostUpdated(UUID tenantId, UUID postId, String title, String slug, String locale);
}
