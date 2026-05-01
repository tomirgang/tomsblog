package de.tomsblog.blogcontent.domain;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.blogcontent.domain.model.Attachment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AttachmentTest {

    @Test
    @DisplayName("Create attachment with show=true generates ID")
    void createAttachmentWithShowTrue() {
        Attachment attachment = Attachment.create("photo.jpg", "image/jpeg", 2048, true);

        assertThat(attachment.id()).isNotNull();
        assertThat(attachment.filename()).isEqualTo("photo.jpg");
        assertThat(attachment.contentType()).isEqualTo("image/jpeg");
        assertThat(attachment.size()).isEqualTo(2048);
        assertThat(attachment.show()).isTrue();
        assertThat(attachment.storageKey()).isNull();
    }

    @Test
    @DisplayName("Create embedded attachment with show=false")
    void createEmbeddedAttachmentWithShowFalse() {
        Attachment attachment = Attachment.create("inline-image.png", "image/png", 512, false);

        assertThat(attachment.show()).isFalse();
    }

    @Test
    @DisplayName("Attachment withStorageKey returns new instance with key")
    void withStorageKeyReturnsNewInstance() {
        Attachment original = Attachment.create("photo.jpg", "image/jpeg", 2048, true);
        Attachment uploaded = original.withStorageKey("bucket/tenant/photo.jpg");

        assertThat(uploaded.storageKey()).isEqualTo("bucket/tenant/photo.jpg");
        assertThat(uploaded.id()).isEqualTo(original.id());
        assertThat(uploaded.show()).isTrue();
        assertThat(original.storageKey()).isNull();
    }

    @Test
    @DisplayName("Attachment with blank filename throws exception")
    void attachmentWithBlankFilenameThrows() {
        assertThatThrownBy(() -> Attachment.create("", "image/jpeg", 1024, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Filename must not be blank");
    }

    @Test
    @DisplayName("Attachment with blank contentType throws exception")
    void attachmentWithBlankContentTypeThrows() {
        assertThatThrownBy(() -> Attachment.create("file.pdf", "", 1024, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ContentType must not be blank");
    }

    @Test
    @DisplayName("Attachment with negative size throws exception")
    void attachmentWithNegativeSizeThrows() {
        assertThatThrownBy(() -> Attachment.create("file.pdf", "application/pdf", -1, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Size must not be negative");
    }

    @Test
    @DisplayName("Attachment with zero size is valid")
    void attachmentWithZeroSizeIsValid() {
        Attachment attachment = Attachment.create("empty.txt", "text/plain", 0, false);
        assertThat(attachment.size()).isZero();
    }
}
