package de.tomsblog.blogcontent.adapter.outbound.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPostRepository extends JpaRepository<PostJpaEntity, UUID> {

    Optional<PostJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<PostJpaEntity> findAllByTenantId(UUID tenantId);

    List<PostJpaEntity> findAllByTenantIdAndStatusOrderByPublishedAtDesc(UUID tenantId, PostStatusJpa status);

    List<PostJpaEntity> findAllByTenantIdAndStatusOrderByPublishedAtDesc(
            UUID tenantId, PostStatusJpa status, Pageable pageable);

    Optional<PostJpaEntity> findBySlugAndTenantId(String slug, UUID tenantId);

    void deleteByIdAndTenantId(UUID id, UUID tenantId);
}
