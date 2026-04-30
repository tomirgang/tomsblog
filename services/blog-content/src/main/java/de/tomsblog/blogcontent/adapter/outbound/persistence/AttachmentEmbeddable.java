package de.tomsblog.blogcontent.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public class AttachmentEmbeddable {

    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size", nullable = false)
    private long size;

    @Column(name = "show", nullable = false)
    private boolean show;

    @Column(name = "storage_key")
    private String storageKey;

    protected AttachmentEmbeddable() {
    }

    public AttachmentEmbeddable(UUID id, String filename, String contentType, long size, boolean show,
            String storageKey) {
        this.id = id;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.show = show;
        this.storageKey = storageKey;
    }

    public UUID getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public boolean isShow() {
        return show;
    }

    public String getStorageKey() {
        return storageKey;
    }
}
