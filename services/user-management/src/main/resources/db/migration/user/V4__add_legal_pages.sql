-- Add legal page content fields to tenant_settings
-- @req SWR-054
-- @req SWR-055

ALTER TABLE tenant_settings ADD COLUMN impressum_content TEXT;
ALTER TABLE tenant_settings ADD COLUMN privacy_policy_content TEXT;
