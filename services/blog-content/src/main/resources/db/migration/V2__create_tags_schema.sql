-- V2: Tags table

CREATE TABLE tags (
    id        UUID         NOT NULL PRIMARY KEY,
    tenant_id UUID         NOT NULL,
    name      VARCHAR(100) NOT NULL,
    slug      VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_tags_tenant_name ON tags (tenant_id, name);
CREATE UNIQUE INDEX idx_tags_tenant_slug ON tags (tenant_id, slug);
CREATE INDEX idx_tags_tenant_id ON tags (tenant_id);
