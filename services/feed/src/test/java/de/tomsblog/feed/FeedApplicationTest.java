package de.tomsblog.feed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeedApplicationTest {

    @Test
    @DisplayName("SWR-090: application class can be instantiated")
    void applicationClassExists() {
        new FeedApplication();
    }
}
