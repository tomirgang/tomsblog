package de.tomsblog.blogcontent.domain;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.blogcontent.domain.model.Source;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SourceTest {

    @Test
    @DisplayName("SWR-012: Source creation with valid data succeeds")
    void sourceCreationSucceeds() {
        Source source = new Source("https://example.com/article", "Example Article");

        assertThat(source.url()).isEqualTo("https://example.com/article");
        assertThat(source.title()).isEqualTo("Example Article");
    }

    @Test
    @DisplayName("Source with blank URL throws exception")
    void sourceWithBlankUrlThrows() {
        assertThatThrownBy(() -> new Source("", "Title"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL must not be blank");
    }

    @Test
    @DisplayName("Source with null URL throws exception")
    void sourceWithNullUrlThrows() {
        assertThatThrownBy(() -> new Source(null, "Title")).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Source with blank title throws exception")
    void sourceWithBlankTitleThrows() {
        assertThatThrownBy(() -> new Source("https://example.com", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title must not be blank");
    }

    @Test
    @DisplayName("Source with null title throws exception")
    void sourceWithNullTitleThrows() {
        assertThatThrownBy(() -> new Source("https://example.com", null)).isInstanceOf(NullPointerException.class);
    }
}
