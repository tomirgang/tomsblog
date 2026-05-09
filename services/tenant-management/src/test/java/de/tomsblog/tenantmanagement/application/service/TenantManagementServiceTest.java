package de.tomsblog.tenantmanagement.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tomsblog.shared.audit.AuditLogEntry;
import de.tomsblog.shared.audit.AuditLogger;
import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.tenantmanagement.application.port.outbound.TenantRepository;
import de.tomsblog.tenantmanagement.domain.model.LoginMode;
import de.tomsblog.tenantmanagement.domain.model.Tenant;
import de.tomsblog.tenantmanagement.domain.model.TenantStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TenantManagementServiceTest {

    private TenantRepository repository;
    private AuditLogger auditLogger;
    private TenantManagementService service;

    @BeforeEach
    void setUp() {
        repository = mock(TenantRepository.class);
        auditLogger = mock(AuditLogger.class);
        service = new TenantManagementService(repository, auditLogger);
    }

    @Test
    @DisplayName("SWR-072: createTenant creates and persists a new tenant")
    void createTenantSuccess() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var tenant = service.createTenant("my-blog", "My Blog");

        assertThat(tenant.getSlug()).isEqualTo("my-blog");
        assertThat(tenant.getDisplayName()).isEqualTo("My Blog");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        verify(repository).save(any());
        verify(auditLogger).log(any(AuditLogEntry.class));
    }

    @Test
    @DisplayName("SWR-072: getTenant returns existing tenant")
    void getTenantSuccess() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.create(tenantId, "s", "Name");
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));

        var result = service.getTenant(tenantId);

        assertThat(result.getDisplayName()).isEqualTo("Name");
    }

    @Test
    @DisplayName("SWR-072: getTenant throws for missing tenant")
    void getTenantNotFound() {
        var tenantId = TenantId.generate();
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTenant(tenantId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("SWR-072: updateGeneralSettings updates and persists")
    void updateGeneralSettingsSuccess() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.create(tenantId, "s", "Old");
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateGeneralSettings(tenantId, "New", "Tag", "INTERNAL", false, Set.of());

        assertThat(result.getDisplayName()).isEqualTo("New");
        assertThat(result.getLoginMode()).isEqualTo(LoginMode.INTERNAL);
        verify(auditLogger).log(any(AuditLogEntry.class));
    }

    @Test
    @DisplayName("SWR-072: updateGeneralSettings validates OIDC config")
    void updateGeneralSettingsValidatesOidc() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.create(tenantId, "s", "Name");
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.updateGeneralSettings(tenantId, "Name", null, "OIDC", false, Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oidcIssuerUrl");
    }

    @Test
    @DisplayName("SWR-072: updateOidcSettings updates and persists")
    void updateOidcSettingsSuccess() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.create(tenantId, "s", "Name");
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateOidcSettings(tenantId, "https://issuer", "cid", "cs");

        assertThat(result.getOidcIssuerUrl()).isEqualTo("https://issuer");
        verify(auditLogger).log(any(AuditLogEntry.class));
    }

    @Test
    @DisplayName("SWR-072: updateLegalSettings updates and persists")
    void updateLegalSettingsSuccess() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.create(tenantId, "s", "Name");
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateLegalSettings(tenantId, "imp", "priv");

        assertThat(result.getImpressumContent()).isEqualTo("imp");
        verify(auditLogger).log(any(AuditLogEntry.class));
    }

    @Test
    @DisplayName("SWR-072: listAllTenants delegates to repository")
    void listAllTenantsSuccess() {
        var t1 = Tenant.create(TenantId.generate(), "a", "A");
        var t2 = Tenant.create(TenantId.generate(), "b", "B");
        when(repository.findAll()).thenReturn(List.of(t1, t2));

        var result = service.listAllTenants();

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("SWR-072: updateOidcSettings validates when OIDC required")
    void updateOidcSettingsValidates() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.create(tenantId, "s", "Name");
        tenant.updateGeneralSettings("Name", null, LoginMode.OIDC, false, Set.of());
        tenant.updateOidcSettings("https://issuer", "cid", "cs");
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.updateOidcSettings(tenantId, "", "", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oidcIssuerUrl");
    }

    @Test
    @DisplayName("SWR-072: updateGeneralSettings validates OIDC client ID")
    void updateGeneralSettingsValidatesOidcClientId() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.create(tenantId, "s", "Name");
        tenant.updateOidcSettings("https://issuer", null, null);
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.updateGeneralSettings(tenantId, "Name", null, "OIDC", false, Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oidcClientId");
    }

    @Test
    @DisplayName("SWR-072: updateGeneralSettings validates blank OIDC client ID")
    void updateGeneralSettingsValidatesBlankOidcClientId() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.create(tenantId, "s", "Name");
        tenant.updateOidcSettings("https://issuer", " ", null);
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.updateGeneralSettings(tenantId, "Name", null, "OIDC", false, Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oidcClientId");
    }

    @Test
    @DisplayName("SWR-072: updateGeneralSettings validates blank OIDC issuer URL")
    void updateGeneralSettingsValidatesBlankOidcIssuerUrl() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.create(tenantId, "s", "Name");
        tenant.updateOidcSettings(" ", "cid", "cs");
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.updateGeneralSettings(tenantId, "Name", null, "OIDC", false, Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oidcIssuerUrl");
    }

    @Test
    @DisplayName("SWR-072: updateGeneralSettings validates OIDC config for BOTH login mode")
    void updateGeneralSettingsValidatesOidcForBothMode() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.create(tenantId, "s", "Name");
        tenant.updateOidcSettings(null, null, null);
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.updateGeneralSettings(tenantId, "Name", null, "BOTH", false, Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oidcIssuerUrl");
    }

    @Test
    @DisplayName("SWR-072: updateOidcSettings validates when BOTH login mode")
    void updateOidcSettingsValidatesForBothMode() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.create(tenantId, "s", "Name");
        tenant.updateGeneralSettings("Name", null, LoginMode.BOTH, false, Set.of());
        tenant.updateOidcSettings("https://issuer", "cid", "cs");
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.updateOidcSettings(tenantId, "", "", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oidcIssuerUrl");
    }
}
