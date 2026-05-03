package de.tomsblog.usermanagement.application.service;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.application.port.outbound.TenantSettingsRepository;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import java.util.Set;

/**
 * Application service implementing tenant settings use cases.
 *
 * @req SWR-044
 * @req SWR-045
 */
public class TenantSettingsService implements TenantSettingsUseCase {

    private final TenantSettingsRepository repository;

    public TenantSettingsService(TenantSettingsRepository repository) {
        this.repository = repository;
    }

    @Override
    public TenantSettings getSettings(TenantId tenantId) {
        return repository.findByTenantId(tenantId).orElseGet(() -> createDefault(tenantId));
    }

    @Override
    public TenantSettings updateLoginMode(TenantId tenantId, LoginMode loginMode) {
        var settings = getOrCreate(tenantId);
        settings.updateLoginMode(loginMode);
        return repository.save(settings);
    }

    @Override
    public TenantSettings updateAutoApproval(
            TenantId tenantId, boolean autoApproveOidc, Set<String> autoApproveEmailDomains) {
        var settings = getOrCreate(tenantId);
        settings.updateAutoApproveOidc(autoApproveOidc);
        settings.setAutoApproveEmailDomains(autoApproveEmailDomains);
        return repository.save(settings);
    }

    private TenantSettings getOrCreate(TenantId tenantId) {
        return repository.findByTenantId(tenantId).orElseGet(() -> createDefault(tenantId));
    }

    private TenantSettings createDefault(TenantId tenantId) {
        var settings = TenantSettings.create(tenantId);
        return repository.save(settings);
    }
}
