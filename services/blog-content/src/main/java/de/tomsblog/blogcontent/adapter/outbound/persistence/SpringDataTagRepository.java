package de.tomsblog.blogcontent.adapter.outbound.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTagRepository extends JpaRepository<TagJpaEntity, UUID> {

    Optional<TagJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<TagJpaEntity> findAllByTenantId(UUID tenantId);

    Optional<TagJpaEntity> findByNameAndTenantId(String name, UUID tenantId);

    void deleteByIdAndTenantId(UUID id, UUID tenantId);
}
