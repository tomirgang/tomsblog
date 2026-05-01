package de.tomsblog.blogcontent.adapter.outbound.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTranslationRepository extends JpaRepository<TranslationJpaEntity, UUID> {

    Optional<TranslationJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<TranslationJpaEntity> findAllByPostIdAndTenantId(UUID postId, UUID tenantId);

    void deleteByIdAndTenantId(UUID id, UUID tenantId);
}
