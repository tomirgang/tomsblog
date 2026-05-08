CREATE TABLE tenants (
    tenant_id           UUID PRIMARY KEY,
    slug                VARCHAR(255) NOT NULL UNIQUE,
    display_name        VARCHAR(255) NOT NULL DEFAULT 'Toms Blog',
    tagline             VARCHAR(500),
    status              VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    login_mode          VARCHAR(50)  NOT NULL DEFAULT 'BOTH',
    auto_approve_oidc   BOOLEAN      NOT NULL DEFAULT FALSE,
    impressum_content   TEXT,
    privacy_policy_content TEXT,
    oidc_issuer_url     VARCHAR(500),
    oidc_client_id      VARCHAR(255),
    oidc_client_secret  VARCHAR(500),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE tenant_auto_approve_domains (
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    domain    VARCHAR(255) NOT NULL,
    PRIMARY KEY (tenant_id, domain)
);
