package de.tomsblog.feed;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.feed.application.port.inbound.FeedEntryUseCase;
import de.tomsblog.feed.application.port.outbound.FeedEntryRepository;
import de.tomsblog.feed.application.service.FeedEntryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedConfigurationTest {

    @Mock
    private FeedEntryRepository feedEntryRepository;

    @Test
    @DisplayName("SWR-090: configuration wires FeedEntryService as FeedEntryUseCase bean")
    void wiresFeedEntryUseCaseBean() {
        FeedConfiguration config = new FeedConfiguration();

        FeedEntryUseCase useCase = config.feedEntryUseCase(feedEntryRepository);

        assertThat(useCase).isInstanceOf(FeedEntryService.class);
    }
}
