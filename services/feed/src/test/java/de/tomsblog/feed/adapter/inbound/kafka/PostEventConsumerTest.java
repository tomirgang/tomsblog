package de.tomsblog.feed.adapter.inbound.kafka;

import static org.mockito.Mockito.*;

import de.tomsblog.events.EventMetadata;
import de.tomsblog.events.PostPublishedEvent;
import de.tomsblog.events.PostUpdatedEvent;
import de.tomsblog.feed.application.port.inbound.FeedEntryUseCase;
import de.tomsblog.shared.tenant.TenantId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostEventConsumerTest {

    @Mock
    private FeedEntryUseCase feedEntryUseCase;

    private PostEventConsumer consumer;

    private final TenantId tenantId = TenantId.generate();
    private final UUID postId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        consumer = new PostEventConsumer(feedEntryUseCase);
    }

    @Test
    @DisplayName("SWR-090: delegates PostPublishedEvent to use case")
    void delegatesPostPublishedEvent() {
        Instant publishedAt = Instant.now();
        EventMetadata metadata = EventMetadata.now(tenantId, "test");
        PostPublishedEvent event = new PostPublishedEvent(metadata, postId, "my-post", "de", publishedAt);

        consumer.onPostPublished(event);

        verify(feedEntryUseCase).onPostPublished(tenantId.value(), postId, "my-post", "de", publishedAt);
    }

    @Test
    @DisplayName("SWR-090: delegates PostUpdatedEvent to use case")
    void delegatesPostUpdatedEvent() {
        EventMetadata metadata = EventMetadata.now(tenantId, "test");
        PostUpdatedEvent event = new PostUpdatedEvent(metadata, postId, "Updated Title", "updated-slug", "en");

        consumer.onPostUpdated(event);

        verify(feedEntryUseCase).onPostUpdated(tenantId.value(), postId, "Updated Title", "updated-slug", "en");
    }
}
