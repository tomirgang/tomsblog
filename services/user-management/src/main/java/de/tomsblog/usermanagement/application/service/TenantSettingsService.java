package de.tomsblog.usermanagement.application.service;

import de.tomsblog.shared.audit.AuditLogEntry;
import de.tomsblog.shared.audit.AuditLogger;
import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.application.port.outbound.TenantSettingsRepository;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import java.util.List;
import java.util.Set;

/**
 * Application service implementing tenant settings use cases.
 *
 * @req SWR-044
 * @req SWR-045
 * @req SWR-050
 * @req SWR-052
 * @req SWR-053
 * @req SWR-056
 * @req SWR-061
 */
public class TenantSettingsService implements TenantSettingsUseCase {

    private final TenantSettingsRepository repository;
    private final AuditLogger auditLogger;

    public TenantSettingsService(TenantSettingsRepository repository, AuditLogger auditLogger) {
        this.repository = repository;
        this.auditLogger = auditLogger;
    }

    @Override
    public TenantSettings getSettings(TenantId tenantId) {
        return repository.findByTenantId(tenantId).orElseGet(() -> createDefault(tenantId));
    }

    @Override
    public TenantSettings updateLoginMode(TenantId tenantId, LoginMode loginMode) {
        var settings = getOrCreate(tenantId);
        settings.updateLoginMode(loginMode);
        TenantSettings saved = repository.save(settings);
        auditLogger.log(AuditLogEntry.create(
                tenantId.toString(),
                "system",
                "TENANT_SETTINGS_UPDATED",
                "TenantSettings",
                tenantId.toString(),
                "loginMode=" + loginMode));
        return saved;
    }

    @Override
    public TenantSettings updateAutoApproval(
            TenantId tenantId, boolean autoApproveOidc, Set<String> autoApproveEmailDomains) {
        var settings = getOrCreate(tenantId);
        settings.updateAutoApproveOidc(autoApproveOidc);
        settings.setAutoApproveEmailDomains(autoApproveEmailDomains);
        TenantSettings saved = repository.save(settings);
        auditLogger.log(AuditLogEntry.create(
                tenantId.toString(), "system", "TENANT_SETTINGS_UPDATED", "TenantSettings", tenantId.toString()));
        return saved;
    }

    @Override
    public TenantSettings updateSettings(
            TenantId tenantId,
            LoginMode loginMode,
            boolean autoApproveOidc,
            Set<String> autoApproveEmailDomains,
            String displayName,
            String tagline,
            String impressumContent,
            String privacyPolicyContent,
            String oidcIssuerUrl,
            String oidcClientId,
            String oidcClientSecret) {
        var settings = getOrCreate(tenantId);
        settings.updateLoginMode(loginMode);
        settings.updateAutoApproveOidc(autoApproveOidc);
        settings.setAutoApproveEmailDomains(autoApproveEmailDomains);
        settings.updateDisplayName(displayName);
        settings.updateTagline(tagline);
        settings.updateImpressumContent(impressumContent);
        settings.updatePrivacyPolicyContent(privacyPolicyContent);
        settings.updateOidcIssuerUrl(oidcIssuerUrl);
        settings.updateOidcClientId(oidcClientId);
        if (oidcClientSecret != null && !oidcClientSecret.isEmpty() && !"***".equals(oidcClientSecret)) {
            settings.updateOidcClientSecret(oidcClientSecret);
        }
        validateOidcConfig(settings);
        TenantSettings saved = repository.save(settings);
        auditLogger.log(AuditLogEntry.create(
                tenantId.toString(), "system", "TENANT_SETTINGS_UPDATED", "TenantSettings", tenantId.toString()));
        return saved;
    }

    private void validateOidcConfig(TenantSettings settings) {
        if (settings.getLoginMode() == LoginMode.OIDC || settings.getLoginMode() == LoginMode.BOTH) {
            if (settings.getOidcIssuerUrl() == null
                    || settings.getOidcIssuerUrl().isBlank()) {
                throw new IllegalArgumentException(
                        "oidcIssuerUrl is required when loginMode is " + settings.getLoginMode());
            }
            if (settings.getOidcClientId() == null || settings.getOidcClientId().isBlank()) {
                throw new IllegalArgumentException(
                        "oidcClientId is required when loginMode is " + settings.getLoginMode());
            }
        }
    }

    @Override
    public List<TenantSettings> listAllTenants() {
        return repository.findAll();
    }

    private TenantSettings getOrCreate(TenantId tenantId) {
        return repository.findByTenantId(tenantId).orElseGet(() -> createDefault(tenantId));
    }

    private TenantSettings createDefault(TenantId tenantId) {
        var settings = TenantSettings.create(tenantId);
        return repository.save(settings);
    }
}
