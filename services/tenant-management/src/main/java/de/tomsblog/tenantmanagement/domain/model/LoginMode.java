package de.tomsblog.tenantmanagement.domain.model;

/**
 * Login mode configuration for a tenant (SWR-044).
 *
 * @req SWR-044
 * @req SWR-072
 */
public enum LoginMode {
    INTERNAL,
    OIDC,
    BOTH
}
