CREATE TABLE audit_log (
    id          UUID PRIMARY KEY,
    timestamp   TIMESTAMPTZ NOT NULL,
    tenant_id   VARCHAR(255),
    actor       VARCHAR(255) NOT NULL,
    action      VARCHAR(255) NOT NULL,
    entity_type VARCHAR(255) NOT NULL,
    entity_id   VARCHAR(255) NOT NULL,
    details     TEXT
);

CREATE INDEX idx_audit_log_tenant_id ON audit_log(tenant_id);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp);
