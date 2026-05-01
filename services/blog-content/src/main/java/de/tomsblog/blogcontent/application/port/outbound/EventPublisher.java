package de.tomsblog.blogcontent.application.port.outbound;

import de.tomsblog.shared.domain.DomainEvent;
import java.util.List;

/** @req SWR-009 */
public interface EventPublisher {

    void publish(List<DomainEvent> events);
}
