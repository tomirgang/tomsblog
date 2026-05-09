package de.tomsblog.blogcontent.adapter.outbound.rabbitmq;

import de.tomsblog.blogcontent.application.port.outbound.SnapshotTaskMessage;
import de.tomsblog.blogcontent.application.port.outbound.TranslationTaskMessage;
import de.tomsblog.blogcontent.application.port.outbound.TtsGenerationTaskMessage;
import de.tomsblog.shared.tenant.TenantId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoggingTaskPublisherTest {

    private final LoggingTaskPublisher publisher = new LoggingTaskPublisher();
    private final TenantId tenantId = TenantId.generate();
    private final UUID postId = UUID.randomUUID();

    @Test
    @DisplayName("SWR-089: publishTranslationTask logs without error")
    void publishTranslationTask_logsWithoutError() {
        publisher.publishTranslationTask(new TranslationTaskMessage(postId, tenantId, "de", "en", "Content"));
    }

    @Test
    @DisplayName("SWR-089: publishTtsTask logs without error")
    void publishTtsTask_logsWithoutError() {
        publisher.publishTtsTask(new TtsGenerationTaskMessage(postId, tenantId, "de", "Title"));
    }

    @Test
    @DisplayName("SWR-089: publishSnapshotTask logs without error")
    void publishSnapshotTask_logsWithoutError() {
        publisher.publishSnapshotTask(new SnapshotTaskMessage(postId, tenantId, "https://example.com", "Example"));
    }
}
