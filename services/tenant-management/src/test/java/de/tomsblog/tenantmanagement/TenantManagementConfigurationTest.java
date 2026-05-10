package de.tomsblog.tenantmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import de.tomsblog.shared.audit.AuditLogger;
import de.tomsblog.tenantmanagement.application.port.outbound.TenantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TenantManagementConfigurationTest {

    @Test
    @DisplayName("SWR-072: configuration creates TenantManagementUseCase bean")
    void createsTenantManagementUseCaseBean() {
        var config = new TenantManagementConfiguration();
        var useCase = config.tenantManagementUseCase(mock(TenantRepository.class), mock(AuditLogger.class));
        assertThat(useCase).isNotNull();
    }
}
