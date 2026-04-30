package de.tomsblog.blogcontent;

import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.application.port.outbound.EventPublisher;
import de.tomsblog.blogcontent.application.port.outbound.PostRepository;
import de.tomsblog.blogcontent.application.service.PostService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BlogContentConfiguration {

    @Bean
    public PostUseCase postUseCase(PostRepository postRepository, EventPublisher eventPublisher) {
        return new PostService(postRepository, eventPublisher);
    }
}
