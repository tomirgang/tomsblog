-- User Management Service schema
-- @req SWR-043

CREATE TABLE user_profiles (
    id            UUID PRIMARY KEY,
    oidc_subject  VARCHAR(255) UNIQUE,
    username      VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),
    auth_source   VARCHAR(50)  NOT NULL,
    email         VARCHAR(255),
    display_name  VARCHAR(255),
    approval_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE,
    updated_at    TIMESTAMP WITH TIME ZONE
);

CREATE TABLE user_global_roles (
    user_profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    role            VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_profile_id, role)
);

CREATE TABLE user_tenant_memberships (
    user_profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    tenant_id       UUID NOT NULL,
    role            VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_profile_id, tenant_id)
);
