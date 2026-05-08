package de.tomsblog.blogcontent.application.port.outbound;

import de.tomsblog.blogcontent.application.port.inbound.AuditLogSearchCriteria;
import de.tomsblog.shared.audit.AuditLogEntry;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Outbound port for querying audit log entries from the persistence layer.
 *
 * @req SWR-087
 * @req SWA-036
 */
public interface AuditLogQueryRepository {

    Page<AuditLogEntry> findByTenantId(String tenantId, Pageable pageable);

    Page<AuditLogEntry> findByTenantIdAndCriteria(String tenantId, AuditLogSearchCriteria criteria, Pageable pageable);

    List<String> findDistinctActions(String tenantId);

    List<String> findDistinctEntityTypes(String tenantId);

    List<String> findDistinctActors(String tenantId);
}
