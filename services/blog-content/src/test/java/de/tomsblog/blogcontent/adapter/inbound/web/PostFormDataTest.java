package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostFormDataTest {

    @Test
    @DisplayName("SWR-027: Constructor with null tagIds defaults to empty list")
    void constructor_nullTagIds_defaultsToEmptyList() {
        PostFormData formData =
                new PostFormData("title", "content", "HTML", "en", null, null, null, null, null, null, null);

        assertThat(formData.getTagIds()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("SWR-027: Constructor with tagIds preserves the list")
    void constructor_withTagIds_preservesList() {
        PostFormData formData = new PostFormData(
                "title", "content", "HTML", "en", null, null, null, null, null, null, java.util.List.of("tag1"));

        assertThat(formData.getTagIds()).containsExactly("tag1");
    }

    @Test
    @DisplayName("SWR-027: Default constructor sets contentType to HTML")
    void defaultConstructor_setsContentTypeToHtml() {
        PostFormData formData = new PostFormData();

        assertThat(formData.getContentType()).isEqualTo("HTML");
    }
}
