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

    @Test
    @DisplayName("SWR-040: setFeaturedFrom and getFeaturedFrom work correctly")
    void setFeaturedFrom_setAndGet() {
        PostFormData formData = new PostFormData();
        formData.setFeaturedFrom("2026-01-01");

        assertThat(formData.getFeaturedFrom()).isEqualTo("2026-01-01");
    }

    @Test
    @DisplayName("SWR-040: setFeaturedUntil and getFeaturedUntil work correctly")
    void setFeaturedUntil_setAndGet() {
        PostFormData formData = new PostFormData();
        formData.setFeaturedUntil("2026-12-31");

        assertThat(formData.getFeaturedUntil()).isEqualTo("2026-12-31");
    }

    @Test
    @DisplayName("SWR-041: setSocialMediaTitle and getSocialMediaTitle work correctly")
    void setSocialMediaTitle_setAndGet() {
        PostFormData formData = new PostFormData();
        formData.setSocialMediaTitle("My Social Title");

        assertThat(formData.getSocialMediaTitle()).isEqualTo("My Social Title");
    }

    @Test
    @DisplayName("SWR-041: setSocialMediaSummary and getSocialMediaSummary work correctly")
    void setSocialMediaSummary_setAndGet() {
        PostFormData formData = new PostFormData();
        formData.setSocialMediaSummary("A brief summary");

        assertThat(formData.getSocialMediaSummary()).isEqualTo("A brief summary");
    }
}
