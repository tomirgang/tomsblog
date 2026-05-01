package de.tomsblog.blogcontent.domain;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.blogcontent.domain.model.Slug;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SlugTest {

    @Test
    @DisplayName("Slug from title generates lowercase hyphenated value")
    void slugFromTitle() {
        Slug slug = Slug.fromTitle("My First Post");
        assertThat(slug.value()).isEqualTo("my-first-post");
    }

    @Test
    @DisplayName("Slug from title removes special characters")
    void slugFromTitleRemovesSpecialChars() {
        Slug slug = Slug.fromTitle("Hello, World! (2024)");
        assertThat(slug.value()).isEqualTo("hello-world-2024");
    }

    @Test
    @DisplayName("Slug constructor rejects null")
    void slugRejectsNull() {
        assertThatThrownBy(() -> new Slug(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Slug constructor rejects blank string")
    void slugRejectsBlank() {
        assertThatThrownBy(() -> new Slug(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    @DisplayName("Slug constructor rejects invalid pattern")
    void slugRejectsInvalidPattern() {
        assertThatThrownBy(() -> new Slug("UPPER-case"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must match pattern");
    }

    @Test
    @DisplayName("Slug constructor accepts valid slug")
    void slugAcceptsValid() {
        Slug slug = new Slug("valid-slug-123");
        assertThat(slug.value()).isEqualTo("valid-slug-123");
    }

    @Test
    @DisplayName("Slug fromTitle collapses multiple hyphens")
    void slugCollapsesMultipleHyphens() {
        Slug slug = Slug.fromTitle("hello   world");
        assertThat(slug.value()).isEqualTo("hello-world");
    }
}
