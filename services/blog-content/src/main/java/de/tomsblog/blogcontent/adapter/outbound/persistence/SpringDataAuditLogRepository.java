package de.tomsblog.blogcontent.adapter.outbound.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for audit log entries.
 *
 * @req SWR-056
 */
public interface SpringDataAuditLogRepository extends JpaRepository<AuditLogJpaEntity, UUID> {}
