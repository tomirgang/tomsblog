package de.tomsblog.blogcontent.adapter.outbound.kafka;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.blogcontent.domain.event.PostCreatedEvent;
import de.tomsblog.blogcontent.domain.event.PostPublishedEvent;
import de.tomsblog.blogcontent.domain.event.PostUpdatedEvent;
import de.tomsblog.blogcontent.domain.event.TranslationApprovedEvent;
import de.tomsblog.blogcontent.domain.event.TranslationCreatedEvent;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.PostLocale;
import de.tomsblog.blogcontent.domain.model.TranslationId;
import de.tomsblog.blogcontent.domain.model.TranslationSource;
import de.tomsblog.shared.domain.DomainEvent;
import de.tomsblog.shared.tenant.TenantId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DomainEventMapperTest {

    private final TenantId tenantId = TenantId.generate();
    private final PostId postId = PostId.generate();

    @Test
    @DisplayName("SWR-009: maps PostCreatedEvent to contract event with tenant key")
    void mapsPostCreatedEvent() {
        PostCreatedEvent domain = PostCreatedEvent.of(postId, tenantId, "My Title", "my-title", "de");

        Optional<DomainEventMapper.MappedEvent> result = DomainEventMapper.toMappedEvent(domain);

        assertThat(result).isPresent();
        assertThat(result.get().contractEvent()).isInstanceOf(de.tomsblog.events.PostCreatedEvent.class);
        assertThat(result.get().tenantKey()).isEqualTo(tenantId.toString());
        var contract = (de.tomsblog.events.PostCreatedEvent) result.get().contractEvent();
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
    @DisplayName("SWR-009: maps PostUpdatedEvent to contract event with tenant key")
    void mapsPostUpdatedEvent() {
        PostUpdatedEvent domain = PostUpdatedEvent.of(postId, tenantId, "Updated Title", "updated-title", "en");

        Optional<DomainEventMapper.MappedEvent> result = DomainEventMapper.toMappedEvent(domain);

        assertThat(result).isPresent();
        assertThat(result.get().contractEvent()).isInstanceOf(de.tomsblog.events.PostUpdatedEvent.class);
        assertThat(result.get().tenantKey()).isEqualTo(tenantId.toString());
        var contract = (de.tomsblog.events.PostUpdatedEvent) result.get().contractEvent();
        assertThat(contract.postId()).isEqualTo(postId.value());
        assertThat(contract.title()).isEqualTo("Updated Title");
        assertThat(contract.slug()).isEqualTo("updated-title");
        assertThat(contract.locale()).isEqualTo("en");
        assertThat(contract.metadata().tenantId()).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("SWR-009: maps PostPublishedEvent to contract event with tenant key")
    void mapsPostPublishedEvent() {
        Instant publishedAt = Instant.now();
        PostPublishedEvent domain = PostPublishedEvent.of(postId, tenantId, "my-title", "de", publishedAt);

        Optional<DomainEventMapper.MappedEvent> result = DomainEventMapper.toMappedEvent(domain);

        assertThat(result).isPresent();
        assertThat(result.get().contractEvent()).isInstanceOf(de.tomsblog.events.PostPublishedEvent.class);
        assertThat(result.get().tenantKey()).isEqualTo(tenantId.toString());
        var contract = (de.tomsblog.events.PostPublishedEvent) result.get().contractEvent();
        assertThat(contract.postId()).isEqualTo(postId.value());
        assertThat(contract.slug()).isEqualTo("my-title");
        assertThat(contract.locale()).isEqualTo("de");
        assertThat(contract.publishedAt()).isEqualTo(publishedAt);
        assertThat(contract.metadata().tenantId()).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("SWR-009: returns empty for TranslationCreatedEvent (local-only)")
    void returnsEmptyForTranslationCreatedEvent() {
        TranslationCreatedEvent domain = TranslationCreatedEvent.of(
                TranslationId.generate(), postId, tenantId, PostLocale.of("de"), TranslationSource.MANUAL);

        Optional<DomainEventMapper.MappedEvent> result = DomainEventMapper.toMappedEvent(domain);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("SWR-009: returns empty for TranslationApprovedEvent (local-only)")
    void returnsEmptyForTranslationApprovedEvent() {
        TranslationApprovedEvent domain =
                TranslationApprovedEvent.of(TranslationId.generate(), postId, tenantId, PostLocale.of("de"));

        Optional<DomainEventMapper.MappedEvent> result = DomainEventMapper.toMappedEvent(domain);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("SWR-009: returns empty for unknown domain event type")
    void returnsEmptyForUnknownEvent() {
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

        Optional<DomainEventMapper.MappedEvent> result = DomainEventMapper.toMappedEvent(unknown);

        assertThat(result).isEmpty();
    }
}
