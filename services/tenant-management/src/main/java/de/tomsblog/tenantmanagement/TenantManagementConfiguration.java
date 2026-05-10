package de.tomsblog.tenantmanagement;

import de.tomsblog.shared.audit.AuditLogger;
import de.tomsblog.tenantmanagement.application.port.inbound.TenantManagementUseCase;
import de.tomsblog.tenantmanagement.application.port.outbound.TenantRepository;
import de.tomsblog.tenantmanagement.application.service.TenantManagementService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantManagementConfiguration {

    @Bean
    public TenantManagementUseCase tenantManagementUseCase(TenantRepository tenantRepository, AuditLogger auditLogger) {
        return new TenantManagementService(tenantRepository, auditLogger);
    }
}
