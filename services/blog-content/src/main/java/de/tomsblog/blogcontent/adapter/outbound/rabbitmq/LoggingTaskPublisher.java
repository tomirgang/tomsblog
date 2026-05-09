package de.tomsblog.blogcontent.adapter.outbound.rabbitmq;

import de.tomsblog.blogcontent.application.port.outbound.SnapshotTaskMessage;
import de.tomsblog.blogcontent.application.port.outbound.TaskPublisher;
import de.tomsblog.blogcontent.application.port.outbound.TranslationTaskMessage;
import de.tomsblog.blogcontent.application.port.outbound.TtsGenerationTaskMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fallback task publisher that logs tasks. Active when the rabbitmq profile is not enabled.
 *
 * @req SWR-089
 */
@Component
@Profile("!rabbitmq")
public class LoggingTaskPublisher implements TaskPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingTaskPublisher.class);

    @Override
    public void publishTranslationTask(TranslationTaskMessage message) {
        log.info(
                "Task (logged): translation for post {} ({} -> {})",
                message.postId(),
                message.sourceLocale(),
                message.targetLocale());
    }

    @Override
    public void publishTtsTask(TtsGenerationTaskMessage message) {
        log.info("Task (logged): TTS for post {} (locale {})", message.postId(), message.locale());
    }

    @Override
    public void publishSnapshotTask(SnapshotTaskMessage message) {
        log.info("Task (logged): snapshot for post {} (url {})", message.postId(), message.url());
    }
}
