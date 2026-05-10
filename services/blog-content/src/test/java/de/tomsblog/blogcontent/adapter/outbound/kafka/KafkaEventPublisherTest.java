package de.tomsblog.blogcontent.adapter.outbound.kafka;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import de.tomsblog.blogcontent.domain.event.PostCreatedEvent;
import de.tomsblog.blogcontent.domain.event.PostPublishedEvent;
import de.tomsblog.blogcontent.domain.event.PostUpdatedEvent;
import de.tomsblog.blogcontent.domain.event.TranslationCreatedEvent;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.PostLocale;
import de.tomsblog.blogcontent.domain.model.TranslationId;
import de.tomsblog.blogcontent.domain.model.TranslationSource;
import de.tomsblog.shared.tenant.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private KafkaEventPublisher publisher;

    private final TenantId tenantId = TenantId.generate();
    private final PostId postId = PostId.generate();

    @BeforeEach
    void setUp() {
        publisher = new KafkaEventPublisher(kafkaTemplate);
    }

    @SuppressWarnings("unchecked")
    private void mockSuccessfulSend() {
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("test", 0), 0, 0, 0, 0, 0);
        SendResult<String, Object> sendResult =
                new SendResult<>(new ProducerRecord<>("test", "key", "value"), metadata);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
    }

    @Test
    @DisplayName("SWR-009: publishes PostCreatedEvent to post.created topic")
    void publishesPostCreatedEvent() {
        mockSuccessfulSend();
        PostCreatedEvent event = PostCreatedEvent.of(postId, tenantId, "Title", "title", "de");

        publisher.publish(List.of(event));

        verify(kafkaTemplate).send(eq("post.created"), eq(tenantId.toString()), any());
    }

    @Test
    @DisplayName("SWR-009: publishes PostUpdatedEvent to post.updated topic")
    void publishesPostUpdatedEvent() {
        mockSuccessfulSend();
        PostUpdatedEvent event = PostUpdatedEvent.of(postId, tenantId, "Title", "title", "de");

        publisher.publish(List.of(event));

        verify(kafkaTemplate).send(eq("post.updated"), eq(tenantId.toString()), any());
    }

    @Test
    @DisplayName("SWR-009: publishes PostPublishedEvent to post.published topic")
    void publishesPostPublishedEvent() {
        mockSuccessfulSend();
        PostPublishedEvent event = PostPublishedEvent.of(postId, tenantId, "title", "de", Instant.now());

        publisher.publish(List.of(event));

        verify(kafkaTemplate).send(eq("post.published"), eq(tenantId.toString()), any());
    }

    @Test
    @DisplayName("SWR-009: publishes multiple events in order")
    void publishesMultipleEvents() {
        mockSuccessfulSend();
        PostCreatedEvent created = PostCreatedEvent.of(postId, tenantId, "Title", "title", "de");
        PostUpdatedEvent updated = PostUpdatedEvent.of(postId, tenantId, "New Title", "new-title", "de");

        publisher.publish(List.of(created, updated));

        verify(kafkaTemplate, times(2)).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("SWR-009: handles empty event list without errors")
    void handlesEmptyList() {
        assertThatCode(() -> publisher.publish(List.of())).doesNotThrowAnyException();
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("SWR-009: uses tenantId as Kafka message key")
    void usesTenantIdAsKey() {
        mockSuccessfulSend();
        PostCreatedEvent event = PostCreatedEvent.of(postId, tenantId, "Title", "title", "de");

        publisher.publish(List.of(event));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(anyString(), keyCaptor.capture(), any());
        assertThat(keyCaptor.getValue()).isEqualTo(tenantId.toString());
    }

    @Test
    @DisplayName("SWR-009: sends mapped contract event, not domain event")
    void sendsContractEvent() {
        mockSuccessfulSend();
        PostCreatedEvent event = PostCreatedEvent.of(postId, tenantId, "Title", "title", "de");

        publisher.publish(List.of(event));

        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(anyString(), anyString(), valueCaptor.capture());
        assertThat(valueCaptor.getValue()).isInstanceOf(de.tomsblog.events.PostCreatedEvent.class);
    }

    @Test
    @DisplayName("SWR-009: logs error when Kafka send fails")
    void logsErrorOnSendFailure() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Connection refused")));
        PostCreatedEvent event = PostCreatedEvent.of(postId, tenantId, "Title", "title", "de");

        assertThatCode(() -> publisher.publish(List.of(event))).doesNotThrowAnyException();

        verify(kafkaTemplate).send(eq("post.created"), eq(tenantId.toString()), any());
    }

    @Test
    @DisplayName("SWR-009: skips local-only translation events without sending to Kafka")
    void skipsTranslationEvents() {
        TranslationCreatedEvent event = TranslationCreatedEvent.of(
                TranslationId.generate(), postId, tenantId, PostLocale.of("de"), TranslationSource.MANUAL);

        assertThatCode(() -> publisher.publish(List.of(event))).doesNotThrowAnyException();

        verifyNoInteractions(kafkaTemplate);
    }
}
