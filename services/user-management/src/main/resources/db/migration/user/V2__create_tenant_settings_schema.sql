-- Tenant settings for login mode and auto-approval configuration
-- @req SWR-044
-- @req SWR-045

CREATE TABLE tenant_settings (
    tenant_id          UUID PRIMARY KEY,
    login_mode         VARCHAR(50) NOT NULL DEFAULT 'BOTH',
    auto_approve_oidc  BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE tenant_auto_approve_domains (
    tenant_id  UUID NOT NULL REFERENCES tenant_settings(tenant_id) ON DELETE CASCADE,
    domain     VARCHAR(255) NOT NULL,
    PRIMARY KEY (tenant_id, domain)
);
