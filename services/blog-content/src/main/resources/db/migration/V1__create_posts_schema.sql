-- V1: Initial schema for blog-content service
-- Creates posts table and associated collection tables

CREATE TABLE posts (
    id            UUID         NOT NULL PRIMARY KEY,
    tenant_id     UUID         NOT NULL,
    author_id     UUID         NOT NULL,
    title         VARCHAR(500) NOT NULL,
    slug          VARCHAR(500) NOT NULL,
    content       TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    locale        VARCHAR(10)  NOT NULL DEFAULT 'de',
    published_at  TIMESTAMPTZ,
    social_media_title   VARCHAR(500),
    social_media_summary TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255)
);

CREATE INDEX idx_posts_tenant_id ON posts (tenant_id);
CREATE INDEX idx_posts_tenant_status ON posts (tenant_id, status);
CREATE UNIQUE INDEX idx_posts_tenant_slug ON posts (tenant_id, slug);

CREATE TABLE post_tags (
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    tag_id  UUID NOT NULL,
    PRIMARY KEY (post_id, tag_id)
);

CREATE TABLE post_sources (
    post_id UUID         NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    url     VARCHAR(2000) NOT NULL,
    title   VARCHAR(500)  NOT NULL
);

CREATE INDEX idx_post_sources_post_id ON post_sources (post_id);

CREATE TABLE post_attachments (
    post_id      UUID         NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    id           UUID         NOT NULL,
    filename     VARCHAR(500) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size         BIGINT       NOT NULL,
    show         BOOLEAN      NOT NULL DEFAULT false,
    storage_key  VARCHAR(1000)
);

CREATE INDEX idx_post_attachments_post_id ON post_attachments (post_id);
