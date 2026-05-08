package de.tomsblog.tenantmanagement.application.service;

import de.tomsblog.shared.audit.AuditLogEntry;
import de.tomsblog.shared.audit.AuditLogger;
import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.tenantmanagement.application.port.inbound.TenantManagementUseCase;
import de.tomsblog.tenantmanagement.application.port.outbound.TenantRepository;
import de.tomsblog.tenantmanagement.domain.model.LoginMode;
import de.tomsblog.tenantmanagement.domain.model.Tenant;
import java.util.List;
import java.util.Set;

/**
 * Application service for tenant lifecycle and configuration (ADR-0031).
 *
 * @req SWR-072
 * @req SWR-073
 * @req SWR-074
 */
public class TenantManagementService implements TenantManagementUseCase {

    private final TenantRepository repository;
    private final AuditLogger auditLogger;

    public TenantManagementService(TenantRepository repository, AuditLogger auditLogger) {
        this.repository = repository;
        this.auditLogger = auditLogger;
    }

    @Override
    public Tenant createTenant(String slug, String displayName) {
        var tenantId = TenantId.generate();
        var tenant = Tenant.create(tenantId, slug, displayName);
        Tenant saved = repository.save(tenant);
        auditLogger.log(AuditLogEntry.create(
                tenantId.toString(), "system", "TENANT_CREATED", "Tenant", tenantId.toString(), "slug=" + slug));
        return saved;
    }

    @Override
    public Tenant getTenant(TenantId tenantId) {
        return repository.findByTenantId(tenantId).orElseThrow(() -> tenantNotFound(tenantId));
    }

    @Override
    public Tenant updateGeneralSettings(
            TenantId tenantId,
            String displayName,
            String tagline,
            String loginMode,
            boolean autoApproveOidc,
            Set<String> autoApproveEmailDomains) {
        var tenant = getTenant(tenantId);
        var mode = LoginMode.valueOf(loginMode);
        tenant.updateGeneralSettings(displayName, tagline, mode, autoApproveOidc, autoApproveEmailDomains);
        validateOidcConfig(tenant);
        Tenant saved = repository.save(tenant);
        auditLogger.log(AuditLogEntry.create(
                tenantId.toString(), "system", "TENANT_GENERAL_SETTINGS_UPDATED", "Tenant", tenantId.toString()));
        return saved;
    }

    @Override
    public Tenant updateOidcSettings(
            TenantId tenantId, String oidcIssuerUrl, String oidcClientId, String oidcClientSecret) {
        var tenant = getTenant(tenantId);
        tenant.updateOidcSettings(oidcIssuerUrl, oidcClientId, oidcClientSecret);
        validateOidcConfig(tenant);
        Tenant saved = repository.save(tenant);
        auditLogger.log(AuditLogEntry.create(
                tenantId.toString(), "system", "TENANT_OIDC_SETTINGS_UPDATED", "Tenant", tenantId.toString()));
        return saved;
    }

    @Override
    public Tenant updateLegalSettings(TenantId tenantId, String impressumContent, String privacyPolicyContent) {
        var tenant = getTenant(tenantId);
        tenant.updateLegalSettings(impressumContent, privacyPolicyContent);
        Tenant saved = repository.save(tenant);
        auditLogger.log(AuditLogEntry.create(
                tenantId.toString(), "system", "TENANT_LEGAL_SETTINGS_UPDATED", "Tenant", tenantId.toString()));
        return saved;
    }

    @Override
    public List<Tenant> listAllTenants() {
        return repository.findAll();
    }

    private void validateOidcConfig(Tenant tenant) {
        if (tenant.getLoginMode() == LoginMode.OIDC || tenant.getLoginMode() == LoginMode.BOTH) {
            if (tenant.getOidcIssuerUrl() == null || tenant.getOidcIssuerUrl().isBlank()) {
                throw new IllegalArgumentException(
                        "oidcIssuerUrl is required when loginMode is " + tenant.getLoginMode());
            }
            if (tenant.getOidcClientId() == null || tenant.getOidcClientId().isBlank()) {
                throw new IllegalArgumentException(
                        "oidcClientId is required when loginMode is " + tenant.getLoginMode());
            }
        }
    }

    private static IllegalArgumentException tenantNotFound(TenantId tenantId) {
        return new IllegalArgumentException("Tenant not found: " + tenantId);
    }
}
