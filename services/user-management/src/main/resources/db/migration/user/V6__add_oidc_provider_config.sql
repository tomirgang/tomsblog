-- SWR-061: Add OIDC provider configuration per tenant
ALTER TABLE tenant_settings ADD COLUMN oidc_issuer_url VARCHAR(512);
ALTER TABLE tenant_settings ADD COLUMN oidc_client_id VARCHAR(255);
ALTER TABLE tenant_settings ADD COLUMN oidc_client_secret VARCHAR(512);
