package de.tomsblog.blogcontent.adapter.outbound.rabbitmq;

import de.tomsblog.blogcontent.application.port.outbound.SnapshotTaskMessage;
import de.tomsblog.blogcontent.application.port.outbound.TaskPublisher;
import de.tomsblog.blogcontent.application.port.outbound.TranslationTaskMessage;
import de.tomsblog.blogcontent.application.port.outbound.TtsGenerationTaskMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Publishes task messages to RabbitMQ queues for asynchronous processing.
 *
 * @req SWR-089
 * @req SWA-037
 */
@Component
@Profile("rabbitmq")
public class RabbitMqTaskPublisher implements TaskPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqTaskPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqTaskPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishTranslationTask(TranslationTaskMessage message) {
        log.info(
                "Publishing translation task for post {} (tenant {}, {} -> {})",
                message.postId(),
                message.tenantId(),
                message.sourceLocale(),
                message.targetLocale());
        rabbitTemplate.convertAndSend(
                RabbitMqConfiguration.TASK_EXCHANGE, RabbitMqConfiguration.ROUTING_KEY_TRANSLATION, message);
    }

    @Override
    public void publishTtsTask(TtsGenerationTaskMessage message) {
        log.info(
                "Publishing TTS task for post {} (tenant {}, locale {})",
                message.postId(),
                message.tenantId(),
                message.locale());
        rabbitTemplate.convertAndSend(
                RabbitMqConfiguration.TASK_EXCHANGE, RabbitMqConfiguration.ROUTING_KEY_TTS, message);
    }

    @Override
    public void publishSnapshotTask(SnapshotTaskMessage message) {
        log.info(
                "Publishing snapshot task for post {} (tenant {}, url {})",
                message.postId(),
                message.tenantId(),
                message.url());
        rabbitTemplate.convertAndSend(
                RabbitMqConfiguration.TASK_EXCHANGE, RabbitMqConfiguration.ROUTING_KEY_SNAPSHOT, message);
    }
}
