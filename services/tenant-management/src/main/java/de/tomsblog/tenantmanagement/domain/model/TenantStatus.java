package de.tomsblog.tenantmanagement.domain.model;

/**
 * Lifecycle status of a tenant (ADR-0031).
 *
 * @req SWR-072
 */
public enum TenantStatus {
    ACTIVE,
    SUSPENDED,
    DEACTIVATED
}
