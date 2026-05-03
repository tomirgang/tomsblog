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
    @DisplayName("SWR-012: Source creation with http URL succeeds")
    void sourceWithHttpUrlSucceeds() {
        Source source = new Source("http://example.com/article", "Example Article");
        assertThat(source.url()).isEqualTo("http://example.com/article");
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
    @DisplayName("Source with non-http protocol throws exception")
    void sourceWithJavascriptProtocolThrows() {
        assertThatThrownBy(() -> new Source("javascript:alert(1)", "Title"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("http or https protocol");
    }

    @Test
    @DisplayName("Source with ftp protocol throws exception")
    void sourceWithFtpProtocolThrows() {
        assertThatThrownBy(() -> new Source("ftp://example.com/file", "Title"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("http or https protocol");
    }

    @Test
    @DisplayName("Source with invalid URI throws exception")
    void sourceWithInvalidUriThrows() {
        assertThatThrownBy(() -> new Source("http://invalid url with spaces", "Title"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a valid URI");
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
