-- New tenant settings fields (SWR-091, SWR-092, SWR-094, SWR-095)

ALTER TABLE tenant_settings ADD COLUMN oidc_button_text VARCHAR(100);
ALTER TABLE tenant_settings ADD COLUMN default_role VARCHAR(20) NOT NULL DEFAULT 'READER';
ALTER TABLE tenant_settings ADD COLUMN logo_url TEXT;
ALTER TABLE tenant_settings ADD COLUMN favicon_url TEXT;
ALTER TABLE tenant_settings ADD COLUMN oidc_role_mapping_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE tenant_oidc_role_mappings (
    tenant_id UUID NOT NULL REFERENCES tenant_settings(tenant_id) ON DELETE CASCADE,
    oidc_group VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (tenant_id, oidc_group)
);
