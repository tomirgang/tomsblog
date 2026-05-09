package de.tomsblog.blogcontent.adapter.outbound.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Declares RabbitMQ exchanges, queues, and bindings for task distribution.
 *
 * @req SWA-037
 * @req SWR-089
 */
@Configuration
@Profile("rabbitmq")
public class RabbitMqConfiguration {

    public static final String TASK_EXCHANGE = "tomsblog.tasks";
    public static final String DLX_EXCHANGE = "tomsblog.tasks.dlx";

    public static final String TRANSLATION_QUEUE = "translation.requests";
    public static final String TTS_QUEUE = "tts.generation";
    public static final String SNAPSHOT_QUEUE = "snapshot.requests";

    public static final String TRANSLATION_DLQ = "translation.requests.dlq";
    public static final String TTS_DLQ = "tts.generation.dlq";
    public static final String SNAPSHOT_DLQ = "snapshot.requests.dlq";

    public static final String ROUTING_KEY_TRANSLATION = "task.translation";
    public static final String ROUTING_KEY_TTS = "task.tts";
    public static final String ROUTING_KEY_SNAPSHOT = "task.snapshot";

    @Bean
    TopicExchange taskExchange() {
        return new TopicExchange(TASK_EXCHANGE);
    }

    @Bean
    TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX_EXCHANGE);
    }

    // --- Quorum Queues with Dead-Letter routing ---

    @Bean
    Queue translationQueue() {
        return QueueBuilder.durable(TRANSLATION_QUEUE)
                .quorum()
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(ROUTING_KEY_TRANSLATION)
                .build();
    }

    @Bean
    Queue ttsQueue() {
        return QueueBuilder.durable(TTS_QUEUE)
                .quorum()
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(ROUTING_KEY_TTS)
                .build();
    }

    @Bean
    Queue snapshotQueue() {
        return QueueBuilder.durable(SNAPSHOT_QUEUE)
                .quorum()
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(ROUTING_KEY_SNAPSHOT)
                .build();
    }

    // --- Dead-Letter Queues ---

    @Bean
    Queue translationDlq() {
        return QueueBuilder.durable(TRANSLATION_DLQ).quorum().build();
    }

    @Bean
    Queue ttsDlq() {
        return QueueBuilder.durable(TTS_DLQ).quorum().build();
    }

    @Bean
    Queue snapshotDlq() {
        return QueueBuilder.durable(SNAPSHOT_DLQ).quorum().build();
    }

    // --- Bindings: Task Exchange -> Queues ---

    @Bean
    Binding translationBinding(Queue translationQueue, TopicExchange taskExchange) {
        return BindingBuilder.bind(translationQueue).to(taskExchange).with(ROUTING_KEY_TRANSLATION);
    }

    @Bean
    Binding ttsBinding(Queue ttsQueue, TopicExchange taskExchange) {
        return BindingBuilder.bind(ttsQueue).to(taskExchange).with(ROUTING_KEY_TTS);
    }

    @Bean
    Binding snapshotBinding(Queue snapshotQueue, TopicExchange taskExchange) {
        return BindingBuilder.bind(snapshotQueue).to(taskExchange).with(ROUTING_KEY_SNAPSHOT);
    }

    // --- Bindings: DLX -> Dead-Letter Queues ---

    @Bean
    Binding translationDlqBinding(Queue translationDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(translationDlq).to(deadLetterExchange).with(ROUTING_KEY_TRANSLATION);
    }

    @Bean
    Binding ttsDlqBinding(Queue ttsDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(ttsDlq).to(deadLetterExchange).with(ROUTING_KEY_TTS);
    }

    @Bean
    Binding snapshotDlqBinding(Queue snapshotDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(snapshotDlq).to(deadLetterExchange).with(ROUTING_KEY_SNAPSHOT);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
