package de.tomsblog.blogcontent.adapter.outbound.kafka;

import de.tomsblog.blogcontent.application.port.outbound.EventPublisher;
import de.tomsblog.shared.domain.DomainEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events to Kafka topics using JSON serialization.
 *
 * @req SWR-009
 */
@Component
@Profile("kafka")
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            Object contractEvent = DomainEventMapper.toContractEvent(event);
            String topic = event.eventType();
            String key = extractTenantKey(event);

            kafkaTemplate.send(topic, key, contractEvent).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error(
                            "Failed to publish event {} [{}] to topic {}: {}",
                            event.eventType(),
                            event.eventId(),
                            topic,
                            ex.getMessage());
                } else {
                    log.info(
                            "Published event {} [{}] to topic {} partition {}",
                            event.eventType(),
                            event.eventId(),
                            topic,
                            result.getRecordMetadata().partition());
                }
            });
        }
    }

    private String extractTenantKey(DomainEvent event) {
        return switch (event) {
            case de.tomsblog.blogcontent.domain.event.PostCreatedEvent e ->
                e.tenantId().toString();
            case de.tomsblog.blogcontent.domain.event.PostUpdatedEvent e ->
                e.tenantId().toString();
            case de.tomsblog.blogcontent.domain.event.PostPublishedEvent e ->
                e.tenantId().toString();
            default -> null;
        };
    }
}
