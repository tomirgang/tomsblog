package de.tomsblog.blogcontent.adapter.outbound.persistence;

import de.tomsblog.blogcontent.application.port.outbound.TranslationRepository;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.Translation;
import de.tomsblog.blogcontent.domain.model.TranslationId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
@SuppressWarnings("null")
public class JpaTranslationRepository implements TranslationRepository {

    private final SpringDataTranslationRepository springDataTranslationRepository;

    public JpaTranslationRepository(SpringDataTranslationRepository springDataTranslationRepository) {
        this.springDataTranslationRepository = springDataTranslationRepository;
    }

    @Override
    public Translation save(Translation translation) {
        TranslationJpaEntity entity = TranslationMapper.toEntity(translation);
        TranslationJpaEntity saved = springDataTranslationRepository.save(entity);
        return TranslationMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Translation> findByIdAndTenantId(TranslationId id, TenantId tenantId) {
        return springDataTranslationRepository
                .findByIdAndTenantId(id.value(), tenantId.value())
                .map(TranslationMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Translation> findAllByPostIdAndTenantId(PostId postId, TenantId tenantId) {
        return springDataTranslationRepository.findAllByPostIdAndTenantId(postId.value(), tenantId.value()).stream()
                .map(TranslationMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteByIdAndTenantId(TranslationId id, TenantId tenantId) {
        springDataTranslationRepository.deleteByIdAndTenantId(id.value(), tenantId.value());
    }
}
