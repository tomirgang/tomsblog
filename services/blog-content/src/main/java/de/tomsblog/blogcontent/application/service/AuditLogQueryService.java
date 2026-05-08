package de.tomsblog.blogcontent.application.service;

import de.tomsblog.blogcontent.application.port.inbound.AuditLogQueryUseCase;
import de.tomsblog.blogcontent.application.port.inbound.AuditLogSearchCriteria;
import de.tomsblog.blogcontent.application.port.outbound.AuditLogQueryRepository;
import de.tomsblog.shared.audit.AuditLogEntry;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Application service for querying audit log entries.
 *
 * @req SWR-087
 * @req SWA-036
 */
public class AuditLogQueryService implements AuditLogQueryUseCase {

    private final AuditLogQueryRepository auditLogQueryRepository;

    public AuditLogQueryService(AuditLogQueryRepository auditLogQueryRepository) {
        this.auditLogQueryRepository = auditLogQueryRepository;
    }

    @Override
    public Page<AuditLogEntry> findAll(TenantId tenantId, Pageable pageable) {
        return auditLogQueryRepository.findByTenantId(tenantId.value().toString(), pageable);
    }

    @Override
    public Page<AuditLogEntry> search(TenantId tenantId, AuditLogSearchCriteria criteria, Pageable pageable) {
        return auditLogQueryRepository.findByTenantIdAndCriteria(
                tenantId.value().toString(), criteria, pageable);
    }

    @Override
    public List<String> getDistinctActions(TenantId tenantId) {
        return auditLogQueryRepository.findDistinctActions(tenantId.value().toString());
    }

    @Override
    public List<String> getDistinctEntityTypes(TenantId tenantId) {
        return auditLogQueryRepository.findDistinctEntityTypes(tenantId.value().toString());
    }

    @Override
    public List<String> getDistinctActors(TenantId tenantId) {
        return auditLogQueryRepository.findDistinctActors(tenantId.value().toString());
    }
}
