package de.tomsblog.usermanagement.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TenantSettingsMapperTest {

    @Test
    @DisplayName("SWR-044: toEntity maps domain to JPA entity")
    void toEntityMaps() {
        var tenantId = TenantId.generate();
        var settings = TenantSettings.reconstitute(tenantId, LoginMode.OIDC, true, Set.of("example.com", "test.org"));

        var entity = TenantSettingsMapper.toEntity(settings);

        assertThat(entity.getTenantId()).isEqualTo(tenantId.value());
        assertThat(entity.getLoginMode()).isEqualTo("OIDC");
        assertThat(entity.isAutoApproveOidc()).isTrue();
        assertThat(entity.getAutoApproveEmailDomains()).containsExactlyInAnyOrder("example.com", "test.org");
    }

    @Test
    @DisplayName("SWR-044: toDomain maps JPA entity to domain")
    void toDomainMaps() {
        var entity = new TenantSettingsJpaEntity();
        var tenantId = java.util.UUID.randomUUID();
        entity.setTenantId(tenantId);
        entity.setLoginMode("INTERNAL");
        entity.setAutoApproveOidc(false);
        entity.setAutoApproveEmailDomains(Set.of("corp.com"));

        var settings = TenantSettingsMapper.toDomain(entity);

        assertThat(settings.getTenantId().value()).isEqualTo(tenantId);
        assertThat(settings.getLoginMode()).isEqualTo(LoginMode.INTERNAL);
        assertThat(settings.isAutoApproveOidc()).isFalse();
        assertThat(settings.getAutoApproveEmailDomains()).containsExactly("corp.com");
    }

    @Test
    @DisplayName("SWR-044: roundtrip preserves all fields")
    void roundtripPreservesFields() {
        var tenantId = TenantId.generate();
        var original = TenantSettings.reconstitute(tenantId, LoginMode.BOTH, true, Set.of("a.com", "b.com"));

        var entity = TenantSettingsMapper.toEntity(original);
        var restored = TenantSettingsMapper.toDomain(entity);

        assertThat(restored.getTenantId()).isEqualTo(original.getTenantId());
        assertThat(restored.getLoginMode()).isEqualTo(original.getLoginMode());
        assertThat(restored.isAutoApproveOidc()).isEqualTo(original.isAutoApproveOidc());
        assertThat(restored.getAutoApproveEmailDomains()).isEqualTo(original.getAutoApproveEmailDomains());
    }
}
