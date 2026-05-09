package de.tomsblog.blogcontent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.application.port.inbound.TagUseCase;
import de.tomsblog.blogcontent.application.port.inbound.TranslationUseCase;
import de.tomsblog.blogcontent.application.port.outbound.EventPublisher;
import de.tomsblog.blogcontent.application.port.outbound.PostRepository;
import de.tomsblog.blogcontent.application.port.outbound.TagRepository;
import de.tomsblog.blogcontent.application.port.outbound.TaskPublisher;
import de.tomsblog.blogcontent.application.port.outbound.TranslationRepository;
import de.tomsblog.shared.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BlogContentConfigurationTest {

    @Test
    @DisplayName("Configuration creates PostUseCase bean")
    void createsPostUseCase() {
        BlogContentConfiguration config = new BlogContentConfiguration();
        PostRepository postRepository = mock(PostRepository.class);
        EventPublisher eventPublisher = mock(EventPublisher.class);
        TaskPublisher taskPublisher = mock(TaskPublisher.class);
        AuditLogger auditLogger = mock(AuditLogger.class);

        PostUseCase result = config.postUseCase(postRepository, eventPublisher, taskPublisher, auditLogger);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Configuration creates TagUseCase bean")
    void createsTagUseCase() {
        BlogContentConfiguration config = new BlogContentConfiguration();
        TagRepository tagRepository = mock(TagRepository.class);
        AuditLogger auditLogger = mock(AuditLogger.class);

        TagUseCase result = config.tagUseCase(tagRepository, auditLogger);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Configuration creates TranslationUseCase bean")
    void createsTranslationUseCase() {
        BlogContentConfiguration config = new BlogContentConfiguration();
        TranslationRepository translationRepository = mock(TranslationRepository.class);
        EventPublisher eventPublisher = mock(EventPublisher.class);
        AuditLogger auditLogger = mock(AuditLogger.class);

        TranslationUseCase result = config.translationUseCase(translationRepository, eventPublisher, auditLogger);

        assertThat(result).isNotNull();
    }
}
