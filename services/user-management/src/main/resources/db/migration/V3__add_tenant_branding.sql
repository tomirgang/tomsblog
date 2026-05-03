-- Add branding fields to tenant_settings
-- @req SWR-050

ALTER TABLE tenant_settings ADD COLUMN display_name VARCHAR(255) NOT NULL DEFAULT 'Toms Blog';
ALTER TABLE tenant_settings ADD COLUMN tagline VARCHAR(500);
