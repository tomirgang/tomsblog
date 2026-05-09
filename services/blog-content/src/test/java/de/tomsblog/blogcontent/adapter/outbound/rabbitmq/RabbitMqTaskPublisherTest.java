package de.tomsblog.blogcontent.adapter.outbound.rabbitmq;

import static org.mockito.Mockito.*;

import de.tomsblog.blogcontent.application.port.outbound.SnapshotTaskMessage;
import de.tomsblog.blogcontent.application.port.outbound.TranslationTaskMessage;
import de.tomsblog.blogcontent.application.port.outbound.TtsGenerationTaskMessage;
import de.tomsblog.shared.tenant.TenantId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class RabbitMqTaskPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitMqTaskPublisher publisher;

    private final TenantId tenantId = TenantId.generate();
    private final UUID postId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        publisher = new RabbitMqTaskPublisher(rabbitTemplate);
    }

    @Test
    @DisplayName("SWR-089: publishTranslationTask sends to correct exchange and routing key")
    void publishTranslationTask_sendsMessage() {
        TranslationTaskMessage message = new TranslationTaskMessage(postId, tenantId, "de", "en", "Hello World");

        publisher.publishTranslationTask(message);

        verify(rabbitTemplate)
                .convertAndSend(
                        RabbitMqConfiguration.TASK_EXCHANGE, RabbitMqConfiguration.ROUTING_KEY_TRANSLATION, message);
    }

    @Test
    @DisplayName("SWR-089: publishTtsTask sends to correct exchange and routing key")
    void publishTtsTask_sendsMessage() {
        TtsGenerationTaskMessage message = new TtsGenerationTaskMessage(postId, tenantId, "de", "Title");

        publisher.publishTtsTask(message);

        verify(rabbitTemplate)
                .convertAndSend(RabbitMqConfiguration.TASK_EXCHANGE, RabbitMqConfiguration.ROUTING_KEY_TTS, message);
    }

    @Test
    @DisplayName("SWR-089: publishSnapshotTask sends to correct exchange and routing key")
    void publishSnapshotTask_sendsMessage() {
        SnapshotTaskMessage message = new SnapshotTaskMessage(postId, tenantId, "https://example.com", "Example");

        publisher.publishSnapshotTask(message);

        verify(rabbitTemplate)
                .convertAndSend(
                        RabbitMqConfiguration.TASK_EXCHANGE, RabbitMqConfiguration.ROUTING_KEY_SNAPSHOT, message);
    }
}
