package de.tomsblog.blogcontent.application.port.outbound;

import de.tomsblog.shared.domain.DomainEvent;
import java.util.List;

public interface EventPublisher {

    void publish(List<DomainEvent> events);
}
