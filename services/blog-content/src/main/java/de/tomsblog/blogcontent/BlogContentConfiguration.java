package de.tomsblog.blogcontent;

import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.application.port.inbound.TagUseCase;
import de.tomsblog.blogcontent.application.port.inbound.TranslationUseCase;
import de.tomsblog.blogcontent.application.port.outbound.EventPublisher;
import de.tomsblog.blogcontent.application.port.outbound.PostRepository;
import de.tomsblog.blogcontent.application.port.outbound.TagRepository;
import de.tomsblog.blogcontent.application.port.outbound.TranslationRepository;
import de.tomsblog.blogcontent.application.service.PostService;
import de.tomsblog.blogcontent.application.service.TagService;
import de.tomsblog.blogcontent.application.service.TranslationService;
import de.tomsblog.shared.audit.AuditLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BlogContentConfiguration {

    @Bean
    public PostUseCase postUseCase(
            PostRepository postRepository, EventPublisher eventPublisher, AuditLogger auditLogger) {
        return new PostService(postRepository, eventPublisher, auditLogger);
    }

    @Bean
    public TagUseCase tagUseCase(TagRepository tagRepository, AuditLogger auditLogger) {
        return new TagService(tagRepository, auditLogger);
    }

    @Bean
    public TranslationUseCase translationUseCase(
            TranslationRepository translationRepository, EventPublisher eventPublisher, AuditLogger auditLogger) {
        return new TranslationService(translationRepository, eventPublisher, auditLogger);
    }
}
