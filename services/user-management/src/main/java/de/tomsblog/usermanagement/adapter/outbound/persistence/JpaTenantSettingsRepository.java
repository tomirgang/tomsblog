package de.tomsblog.usermanagement.adapter.outbound.persistence;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.outbound.TenantSettingsRepository;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA adapter implementing the tenant settings repository port.
 *
 * @req SWR-044
 * @req SWR-045
 */
@Repository
@Transactional
public class JpaTenantSettingsRepository implements TenantSettingsRepository {

    private final SpringDataTenantSettingsRepository springDataRepository;

    public JpaTenantSettingsRepository(SpringDataTenantSettingsRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public TenantSettings save(TenantSettings tenantSettings) {
        var entity = TenantSettingsMapper.toEntity(tenantSettings);
        var saved = springDataRepository.save(entity);
        return TenantSettingsMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TenantSettings> findByTenantId(TenantId tenantId) {
        return springDataRepository.findByTenantId(tenantId.value()).map(TenantSettingsMapper::toDomain);
    }
}
