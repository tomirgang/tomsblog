-- V3: Translations table

CREATE TABLE translations (
    id            UUID         NOT NULL PRIMARY KEY,
    post_id       UUID         NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    tenant_id     UUID         NOT NULL,
    locale        VARCHAR(10)  NOT NULL,
    title         VARCHAR(500) NOT NULL,
    content       TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    source        VARCHAR(20)  NOT NULL DEFAULT 'MANUAL',
    translated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_translations_post_tenant ON translations (post_id, tenant_id);
CREATE INDEX idx_translations_tenant_id ON translations (tenant_id);
CREATE UNIQUE INDEX idx_translations_post_locale ON translations (post_id, locale);
