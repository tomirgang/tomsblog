package de.tomsblog.blogcontent.adapter.outbound.kafka;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.blogcontent.domain.event.PostCreatedEvent;
import de.tomsblog.blogcontent.domain.event.PostPublishedEvent;
import de.tomsblog.blogcontent.domain.event.PostUpdatedEvent;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.shared.domain.DomainEvent;
import de.tomsblog.shared.tenant.TenantId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DomainEventMapperTest {

    private final TenantId tenantId = TenantId.generate();
    private final PostId postId = PostId.generate();

    @Test
    @DisplayName("SWR-009: maps PostCreatedEvent to contract event")
    void mapsPostCreatedEvent() {
        PostCreatedEvent domain = PostCreatedEvent.of(postId, tenantId, "My Title", "my-title", "de");

        Object result = DomainEventMapper.toContractEvent(domain);

        assertThat(result).isInstanceOf(de.tomsblog.events.PostCreatedEvent.class);
        var contract = (de.tomsblog.events.PostCreatedEvent) result;
        assertThat(contract.postId()).isEqualTo(postId.value());
        assertThat(contract.title()).isEqualTo("My Title");
        assertThat(contract.slug()).isEqualTo("my-title");
        assertThat(contract.locale()).isEqualTo("de");
        assertThat(contract.metadata().tenantId()).isEqualTo(tenantId);
        assertThat(contract.metadata().correlationId())
                .isEqualTo(domain.eventId().toString());
        assertThat(contract.metadata().causedBy()).isEqualTo("PostService");
    }

    @Test
    @DisplayName("SWR-009: maps PostUpdatedEvent to contract event")
    void mapsPostUpdatedEvent() {
        PostUpdatedEvent domain = PostUpdatedEvent.of(postId, tenantId, "Updated Title", "updated-title", "en");

        Object result = DomainEventMapper.toContractEvent(domain);

        assertThat(result).isInstanceOf(de.tomsblog.events.PostUpdatedEvent.class);
        var contract = (de.tomsblog.events.PostUpdatedEvent) result;
        assertThat(contract.postId()).isEqualTo(postId.value());
        assertThat(contract.title()).isEqualTo("Updated Title");
        assertThat(contract.slug()).isEqualTo("updated-title");
        assertThat(contract.locale()).isEqualTo("en");
        assertThat(contract.metadata().tenantId()).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("SWR-009: maps PostPublishedEvent to contract event")
    void mapsPostPublishedEvent() {
        Instant publishedAt = Instant.now();
        PostPublishedEvent domain = PostPublishedEvent.of(postId, tenantId, "my-title", "de", publishedAt);

        Object result = DomainEventMapper.toContractEvent(domain);

        assertThat(result).isInstanceOf(de.tomsblog.events.PostPublishedEvent.class);
        var contract = (de.tomsblog.events.PostPublishedEvent) result;
        assertThat(contract.postId()).isEqualTo(postId.value());
        assertThat(contract.slug()).isEqualTo("my-title");
        assertThat(contract.locale()).isEqualTo("de");
        assertThat(contract.publishedAt()).isEqualTo(publishedAt);
        assertThat(contract.metadata().tenantId()).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("SWR-009: throws for unknown domain event type")
    void throwsForUnknownEvent() {
        DomainEvent unknown = new DomainEvent() {
            @Override
            public UUID eventId() {
                return UUID.randomUUID();
            }

            @Override
            public Instant occurredAt() {
                return Instant.now();
            }

            @Override
            public String eventType() {
                return "unknown";
            }
        };

        assertThatThrownBy(() -> DomainEventMapper.toContractEvent(unknown))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown domain event type");
    }
}
