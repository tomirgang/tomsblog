package de.tomsblog.tenantmanagement.adapter.outbound.persistence;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.tenantmanagement.application.port.outbound.TenantRepository;
import de.tomsblog.tenantmanagement.domain.model.Tenant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA adapter implementing the tenant repository port.
 *
 * @req SWR-072
 */
@Repository
@Transactional
public class JpaTenantRepository implements TenantRepository {

    private final SpringDataTenantRepository springDataRepository;

    public JpaTenantRepository(SpringDataTenantRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Tenant save(Tenant tenant) {
        var entity = TenantMapper.toEntity(tenant);
        var saved = springDataRepository.save(entity);
        return TenantMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tenant> findByTenantId(TenantId tenantId) {
        return springDataRepository.findByTenantId(tenantId.value()).map(TenantMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tenant> findAll() {
        return springDataRepository.findAll().stream()
                .map(TenantMapper::toDomain)
                .toList();
    }
}
