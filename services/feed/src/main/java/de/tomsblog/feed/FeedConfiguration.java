package de.tomsblog.feed;

import de.tomsblog.feed.application.port.inbound.FeedEntryUseCase;
import de.tomsblog.feed.application.port.outbound.FeedEntryRepository;
import de.tomsblog.feed.application.service.FeedEntryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeedConfiguration {

    @Bean
    public FeedEntryUseCase feedEntryUseCase(FeedEntryRepository feedEntryRepository) {
        return new FeedEntryService(feedEntryRepository);
    }
}
