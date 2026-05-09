package de.tomsblog.blogcontent.adapter.outbound.rabbitmq;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

class RabbitMqConfigurationTest {

    private final RabbitMqConfiguration config = new RabbitMqConfiguration();

    @Test
    @DisplayName("SWA-037: taskExchange is created with correct name")
    void taskExchange_hasCorrectName() {
        TopicExchange exchange = config.taskExchange();
        assertThat(exchange.getName()).isEqualTo("tomsblog.tasks");
    }

    @Test
    @DisplayName("SWA-037: deadLetterExchange is created with correct name")
    void deadLetterExchange_hasCorrectName() {
        TopicExchange exchange = config.deadLetterExchange();
        assertThat(exchange.getName()).isEqualTo("tomsblog.tasks.dlx");
    }

    @Test
    @DisplayName("SWA-037: translationQueue is quorum with DLX")
    void translationQueue_isQuorumWithDlx() {
        Queue queue = config.translationQueue();
        assertThat(queue.getName()).isEqualTo("translation.requests");
        assertThat(queue.getArguments()).containsEntry("x-queue-type", "quorum");
        assertThat(queue.getArguments()).containsEntry("x-dead-letter-exchange", "tomsblog.tasks.dlx");
    }

    @Test
    @DisplayName("SWA-037: ttsQueue is quorum with DLX")
    void ttsQueue_isQuorumWithDlx() {
        Queue queue = config.ttsQueue();
        assertThat(queue.getName()).isEqualTo("tts.generation");
        assertThat(queue.getArguments()).containsEntry("x-queue-type", "quorum");
        assertThat(queue.getArguments()).containsEntry("x-dead-letter-exchange", "tomsblog.tasks.dlx");
    }

    @Test
    @DisplayName("SWA-037: snapshotQueue is quorum with DLX")
    void snapshotQueue_isQuorumWithDlx() {
        Queue queue = config.snapshotQueue();
        assertThat(queue.getName()).isEqualTo("snapshot.requests");
        assertThat(queue.getArguments()).containsEntry("x-queue-type", "quorum");
        assertThat(queue.getArguments()).containsEntry("x-dead-letter-exchange", "tomsblog.tasks.dlx");
    }

    @Test
    @DisplayName("SWA-037: dead-letter queues are quorum queues")
    void deadLetterQueues_areQuorum() {
        assertThat(config.translationDlq().getArguments()).containsEntry("x-queue-type", "quorum");
        assertThat(config.ttsDlq().getArguments()).containsEntry("x-queue-type", "quorum");
        assertThat(config.snapshotDlq().getArguments()).containsEntry("x-queue-type", "quorum");
    }

    @Test
    @DisplayName("SWA-037: jsonMessageConverter is Jackson-based")
    void jsonMessageConverter_isJacksonBased() {
        assertThat(config.jsonMessageConverter()).isNotNull();
    }
}
