package de.tomsblog.usermanagement.domain.model;

/**
 * Login mode configuration for a tenant.
 *
 * @req SWR-044
 */
public enum LoginMode {
    INTERNAL,
    OIDC,
    BOTH
}
