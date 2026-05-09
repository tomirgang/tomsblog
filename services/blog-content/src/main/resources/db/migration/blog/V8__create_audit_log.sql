CREATE TABLE audit_log (
    id         UUID         NOT NULL,
    timestamp  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    tenant_id  VARCHAR(255),
    actor      VARCHAR(255) NOT NULL,
    action     VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id  VARCHAR(255) NOT NULL,
    details    TEXT,
    CONSTRAINT pk_audit_log PRIMARY KEY (id)
);

CREATE INDEX idx_audit_log_tenant_timestamp ON audit_log (tenant_id, timestamp);
CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
