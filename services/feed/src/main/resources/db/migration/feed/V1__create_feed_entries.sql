CREATE TABLE feed_entries (
    id         UUID         NOT NULL PRIMARY KEY,
    tenant_id  UUID         NOT NULL,
    post_id    UUID         NOT NULL,
    title      VARCHAR(500),
    slug       VARCHAR(255) NOT NULL,
    locale     VARCHAR(20),
    published_at TIMESTAMPTZ,
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_feed_entries_tenant_post UNIQUE (tenant_id, post_id)
);

CREATE INDEX idx_feed_entries_tenant_id ON feed_entries (tenant_id);
CREATE INDEX idx_feed_entries_published_at ON feed_entries (tenant_id, published_at DESC);
