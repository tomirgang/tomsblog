package de.tomsblog.feed;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

class FeedApplicationTest {

    @Test
    @DisplayName("SWR-090: application class can be instantiated")
    void applicationClassExists() {
        new FeedApplication();
    }

    @Test
    @DisplayName("SWR-090: main method delegates to SpringApplication.run")
    void mainMethod() {
        try (var mocked = mockStatic(SpringApplication.class)) {
            FeedApplication.main(new String[] {});
            mocked.verify(() -> SpringApplication.run(FeedApplication.class, new String[] {}));
        }
    }
}
