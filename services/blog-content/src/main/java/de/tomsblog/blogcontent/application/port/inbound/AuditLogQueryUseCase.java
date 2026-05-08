package de.tomsblog.blogcontent.application.port.inbound;

import de.tomsblog.shared.audit.AuditLogEntry;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Inbound port for querying audit log entries.
 *
 * @req SWR-087
 * @req SWA-036
 */
public interface AuditLogQueryUseCase {

    Page<AuditLogEntry> findAll(TenantId tenantId, Pageable pageable);

    Page<AuditLogEntry> search(TenantId tenantId, AuditLogSearchCriteria criteria, Pageable pageable);

    List<String> getDistinctActions(TenantId tenantId);

    List<String> getDistinctEntityTypes(TenantId tenantId);

    List<String> getDistinctActors(TenantId tenantId);
}
