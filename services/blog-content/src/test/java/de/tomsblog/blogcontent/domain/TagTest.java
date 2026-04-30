package de.tomsblog.blogcontent.domain;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.blogcontent.domain.model.Tag;
import de.tomsblog.blogcontent.domain.model.TagId;
import de.tomsblog.shared.tenant.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TagTest {

    private final TenantId tenantId = TenantId.generate();

    @Test
    @DisplayName("Create tag generates ID and slug")
    void createTagGeneratesIdAndSlug() {
        Tag tag = Tag.create(tenantId, "Spring Boot");

        assertThat(tag.getId()).isNotNull();
        assertThat(tag.getTenantId()).isEqualTo(tenantId);
        assertThat(tag.getName()).isEqualTo("Spring Boot");
        assertThat(tag.getSlug().value()).isEqualTo("spring-boot");
    }

    @Test
    @DisplayName("Create tag with blank name throws exception")
    void createTagWithBlankNameThrows() {
        assertThatThrownBy(() -> Tag.create(tenantId, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tag name must not be blank");
    }

    @Test
    @DisplayName("Create tag with null name throws exception")
    void createTagWithNullNameThrows() {
        assertThatThrownBy(() -> Tag.create(tenantId, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Rename tag updates name and slug")
    void renameTagUpdatesNameAndSlug() {
        Tag tag = Tag.create(tenantId, "Old Name");

        tag.rename("New Name");

        assertThat(tag.getName()).isEqualTo("New Name");
        assertThat(tag.getSlug().value()).isEqualTo("new-name");
    }

    @Test
    @DisplayName("Tag equality is based on ID")
    void tagEqualityIsBasedOnId() {
        Tag tag1 = Tag.create(tenantId, "Java");
        Tag tag2 = Tag.create(tenantId, "Java");

        assertThat(tag1).isNotEqualTo(tag2);
    }

    @Test
    @DisplayName("Reconstitute preserves all fields")
    void reconstitutePreservesFields() {
        TagId id = TagId.generate();
        Tag tag = Tag.reconstitute(id, tenantId, "Kotlin", new de.tomsblog.blogcontent.domain.model.Slug("kotlin"));

        assertThat(tag.getId()).isEqualTo(id);
        assertThat(tag.getTenantId()).isEqualTo(tenantId);
        assertThat(tag.getName()).isEqualTo("Kotlin");
        assertThat(tag.getSlug().value()).isEqualTo("kotlin");
    }
}
