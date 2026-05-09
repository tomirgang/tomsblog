package de.tomsblog.feed.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.tomsblog.feed.application.port.outbound.FeedEntryRepository;
import de.tomsblog.feed.domain.model.FeedEntry;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedEntryServiceTest {

    @Mock
    private FeedEntryRepository feedEntryRepository;

    private FeedEntryService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID postId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new FeedEntryService(feedEntryRepository);
    }

    @Test
    @DisplayName("SWR-090: onPostPublished creates new feed entry when none exists")
    void onPostPublishedCreatesNewEntry() {
        when(feedEntryRepository.findByTenantIdAndPostId(tenantId, postId)).thenReturn(Optional.empty());
        when(feedEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Instant publishedAt = Instant.now();

        service.onPostPublished(tenantId, postId, "my-post", "de", publishedAt);

        ArgumentCaptor<FeedEntry> captor = ArgumentCaptor.forClass(FeedEntry.class);
        verify(feedEntryRepository).save(captor.capture());
        FeedEntry saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getPostId()).isEqualTo(postId);
        assertThat(saved.getSlug()).isEqualTo("my-post");
        assertThat(saved.getLocale()).isEqualTo("de");
        assertThat(saved.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    @DisplayName("SWR-090: onPostPublished updates existing feed entry")
    void onPostPublishedUpdatesExistingEntry() {
        FeedEntry existing = FeedEntry.create(tenantId, postId, "old-slug", "de", Instant.now());
        when(feedEntryRepository.findByTenantIdAndPostId(tenantId, postId)).thenReturn(Optional.of(existing));
        when(feedEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.onPostPublished(tenantId, postId, "new-slug", "en", Instant.now());

        verify(feedEntryRepository).save(existing);
        assertThat(existing.getSlug()).isEqualTo("new-slug");
        assertThat(existing.getLocale()).isEqualTo("en");
    }

    @Test
    @DisplayName("SWR-090: onPostUpdated updates existing feed entry metadata")
    void onPostUpdatedUpdatesExistingEntry() {
        FeedEntry existing = FeedEntry.create(tenantId, postId, "slug", "de", Instant.now());
        when(feedEntryRepository.findByTenantIdAndPostId(tenantId, postId)).thenReturn(Optional.of(existing));
        when(feedEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.onPostUpdated(tenantId, postId, "Updated Title", "updated-slug", "en");

        verify(feedEntryRepository).save(existing);
        assertThat(existing.getTitle()).isEqualTo("Updated Title");
        assertThat(existing.getSlug()).isEqualTo("updated-slug");
        assertThat(existing.getLocale()).isEqualTo("en");
    }

    @Test
    @DisplayName("SWR-090: onPostUpdated ignores event when no feed entry exists")
    void onPostUpdatedIgnoresWhenNoEntry() {
        when(feedEntryRepository.findByTenantIdAndPostId(tenantId, postId)).thenReturn(Optional.empty());

        service.onPostUpdated(tenantId, postId, "Title", "slug", "de");

        verify(feedEntryRepository, never()).save(any());
    }
}
