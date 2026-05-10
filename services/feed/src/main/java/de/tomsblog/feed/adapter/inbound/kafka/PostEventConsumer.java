package de.tomsblog.feed.adapter.inbound.kafka;

import de.tomsblog.events.PostPublishedEvent;
import de.tomsblog.events.PostUpdatedEvent;
import de.tomsblog.feed.application.port.inbound.FeedEntryUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer adapter that listens for post events and delegates to
 * the feed entry use case.
 *
 * @req SWR-090
 */
@Component
@Profile("kafka")
public class PostEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PostEventConsumer.class);

    private final FeedEntryUseCase feedEntryUseCase;

    public PostEventConsumer(FeedEntryUseCase feedEntryUseCase) {
        this.feedEntryUseCase = feedEntryUseCase;
    }

    @KafkaListener(topics = "post.published", groupId = "feed-service")
    public void onPostPublished(PostPublishedEvent event) {
        log.info(
                "Received PostPublishedEvent for post {} in tenant {}",
                event.postId(),
                event.metadata().tenantId());
        feedEntryUseCase.onPostPublished(
                event.metadata().tenantId(), event.postId(), event.slug(), event.locale(), event.publishedAt());
    }

    @KafkaListener(topics = "post.updated", groupId = "feed-service")
    public void onPostUpdated(PostUpdatedEvent event) {
        log.info(
                "Received PostUpdatedEvent for post {} in tenant {}",
                event.postId(),
                event.metadata().tenantId());
        feedEntryUseCase.onPostUpdated(
                event.metadata().tenantId(), event.postId(), event.title(), event.slug(), event.locale());
    }
}
