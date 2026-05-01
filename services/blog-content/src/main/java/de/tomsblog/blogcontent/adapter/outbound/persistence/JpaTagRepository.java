package de.tomsblog.blogcontent.adapter.outbound.persistence;

import de.tomsblog.blogcontent.application.port.outbound.TagRepository;
import de.tomsblog.blogcontent.domain.model.Tag;
import de.tomsblog.blogcontent.domain.model.TagId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
@SuppressWarnings("null")
public class JpaTagRepository implements TagRepository {

    private final SpringDataTagRepository springDataTagRepository;

    public JpaTagRepository(SpringDataTagRepository springDataTagRepository) {
        this.springDataTagRepository = springDataTagRepository;
    }

    @Override
    public Tag save(Tag tag) {
        TagJpaEntity entity = TagMapper.toEntity(tag);
        TagJpaEntity saved = springDataTagRepository.save(entity);
        return TagMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tag> findByIdAndTenantId(TagId id, TenantId tenantId) {
        return springDataTagRepository
                .findByIdAndTenantId(id.value(), tenantId.value())
                .map(TagMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> findAllByTenantId(TenantId tenantId) {
        return springDataTagRepository.findAllByTenantId(tenantId.value()).stream()
                .map(TagMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tag> findByNameAndTenantId(String name, TenantId tenantId) {
        return springDataTagRepository
                .findByNameAndTenantId(name, tenantId.value())
                .map(TagMapper::toDomain);
    }

    @Override
    public void deleteByIdAndTenantId(TagId id, TenantId tenantId) {
        springDataTagRepository.deleteByIdAndTenantId(id.value(), tenantId.value());
    }
}
