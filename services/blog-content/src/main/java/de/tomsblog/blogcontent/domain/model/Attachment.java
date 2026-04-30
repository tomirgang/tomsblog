package de.tomsblog.blogcontent.domain.model;

import java.util.Objects;

/**
 * An attachment associated with a blog post.
 * Attachments with {@code show=true} are displayed in the blog rendering (e.g.
 * gallery).
 * Attachments with {@code show=false} are used for embedded content (e.g.
 * inline images).
 * The storageKey may be null if the file has not yet been uploaded to external
 * storage.
 */
public record Attachment(AttachmentId id, String filename, String contentType, long size, boolean show,
        String storageKey) {

    public Attachment {
        Objects.requireNonNull(id, "AttachmentId must not be null");
        Objects.requireNonNull(filename, "Filename must not be null");
        if (filename.isBlank()) {
            throw new IllegalArgumentException("Filename must not be blank");
        }
        Objects.requireNonNull(contentType, "ContentType must not be null");
        if (contentType.isBlank()) {
            throw new IllegalArgumentException("ContentType must not be blank");
        }
        if (size < 0) {
            throw new IllegalArgumentException("Size must not be negative");
        }
    }

    public static Attachment create(String filename, String contentType, long size, boolean show) {
        return new Attachment(AttachmentId.generate(), filename, contentType, size, show, null);
    }

    public Attachment withStorageKey(String storageKey) {
        return new Attachment(this.id, this.filename, this.contentType, this.size, this.show, storageKey);
    }
}
