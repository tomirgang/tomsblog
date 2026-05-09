package de.tomsblog.blogcontent.application.port.outbound;

/**
 * Outbound port for dispatching task messages to RabbitMQ queues.
 *
 * <p>Unlike {@link EventPublisher} (Kafka, event streaming), this port handles
 * work-queue-style task distribution with guaranteed single processing (ADR-0018).
 *
 * @req SWR-089
 */
public interface TaskPublisher {

    void publishTranslationTask(TranslationTaskMessage message);

    void publishTtsTask(TtsGenerationTaskMessage message);

    void publishSnapshotTask(SnapshotTaskMessage message);
}
