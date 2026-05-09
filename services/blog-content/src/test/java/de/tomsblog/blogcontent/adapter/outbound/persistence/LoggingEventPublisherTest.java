package de.tomsblog.blogcontent.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.blogcontent.domain.event.PostCreatedEvent;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.shared.domain.DomainEvent;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoggingEventPublisherTest {

    private final LoggingEventPublisher publisher = new LoggingEventPublisher();

    @Test
    @DisplayName("publish logs events without throwing")
    void publish_logsEvents() {
        PostId postId = PostId.generate();
        TenantId tenantId = TenantId.generate();
        DomainEvent event = PostCreatedEvent.of(postId, tenantId, "Test Title", "test-title", "de");

        assertThatCode(() -> publisher.publish(List.of(event))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("publish handles empty list")
    void publish_handlesEmptyList() {
        assertThatCode(() -> publisher.publish(List.of())).doesNotThrowAnyException();
    }
}
