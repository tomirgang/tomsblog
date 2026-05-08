package de.tomsblog.blogcontent.adapter.outbound.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for audit log entries.
 *
 * @req SWR-056
 * @req SWR-087
 */
public interface SpringDataAuditLogRepository
        extends JpaRepository<AuditLogJpaEntity, UUID>, JpaSpecificationExecutor<AuditLogJpaEntity> {

    @Query("SELECT DISTINCT a.action FROM AuditLogJpaEntity a WHERE a.tenantId = :tenantId ORDER BY a.action")
    List<String> findDistinctActionsByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT DISTINCT a.entityType FROM AuditLogJpaEntity a WHERE a.tenantId = :tenantId ORDER BY a.entityType")
    List<String> findDistinctEntityTypesByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT DISTINCT a.actor FROM AuditLogJpaEntity a WHERE a.tenantId = :tenantId ORDER BY a.actor")
    List<String> findDistinctActorsByTenantId(@Param("tenantId") String tenantId);
}
