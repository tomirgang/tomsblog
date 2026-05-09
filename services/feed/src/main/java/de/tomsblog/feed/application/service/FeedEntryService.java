package de.tomsblog.feed.application.service;

import de.tomsblog.feed.application.port.inbound.FeedEntryUseCase;
import de.tomsblog.feed.application.port.outbound.FeedEntryRepository;
import de.tomsblog.feed.domain.model.FeedEntry;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application service that processes post events and manages feed entries.
 *
 * @req SWR-090
 */
public class FeedEntryService implements FeedEntryUseCase {

    private static final Logger log = LoggerFactory.getLogger(FeedEntryService.class);

    private final FeedEntryRepository feedEntryRepository;

    public FeedEntryService(FeedEntryRepository feedEntryRepository) {
        this.feedEntryRepository = feedEntryRepository;
    }

    @Override
    public void onPostPublished(UUID tenantId, UUID postId, String slug, String locale, Instant publishedAt) {
        feedEntryRepository
                .findByTenantIdAndPostId(tenantId, postId)
                .ifPresentOrElse(
                        existing -> {
                            existing.update(existing.getTitle(), slug, locale);
                            feedEntryRepository.save(existing);
                            log.info("Updated existing feed entry for post {} in tenant {}", postId, tenantId);
                        },
                        () -> {
                            FeedEntry entry = FeedEntry.create(tenantId, postId, slug, locale, publishedAt);
                            feedEntryRepository.save(entry);
                            log.info("Created feed entry for post {} in tenant {}", postId, tenantId);
                        });
    }

    @Override
    public void onPostUpdated(UUID tenantId, UUID postId, String title, String slug, String locale) {
        feedEntryRepository.findByTenantIdAndPostId(tenantId, postId).ifPresent(entry -> {
            entry.update(title, slug, locale);
            feedEntryRepository.save(entry);
            log.info("Updated feed entry for post {} in tenant {}", postId, tenantId);
        });
    }
}
