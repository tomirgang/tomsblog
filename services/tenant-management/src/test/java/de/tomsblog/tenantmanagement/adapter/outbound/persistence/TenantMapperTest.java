package de.tomsblog.tenantmanagement.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.tenantmanagement.domain.model.LoginMode;
import de.tomsblog.tenantmanagement.domain.model.Tenant;
import de.tomsblog.tenantmanagement.domain.model.TenantStatus;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TenantMapperTest {

    @Test
    @DisplayName("SWR-072: toEntity maps domain to JPA entity")
    void toEntityMaps() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.reconstitute(
                tenantId,
                "my-blog",
                "My Blog",
                "A tagline",
                TenantStatus.ACTIVE,
                LoginMode.OIDC,
                true,
                Set.of("example.com", "test.org"),
                "impressum",
                "privacy",
                "https://issuer",
                "cid",
                "cs");

        var entity = TenantMapper.toEntity(tenant);

        assertThat(entity.getTenantId()).isEqualTo(tenantId.value());
        assertThat(entity.getSlug()).isEqualTo("my-blog");
        assertThat(entity.getDisplayName()).isEqualTo("My Blog");
        assertThat(entity.getTagline()).isEqualTo("A tagline");
        assertThat(entity.getStatus()).isEqualTo("ACTIVE");
        assertThat(entity.getLoginMode()).isEqualTo("OIDC");
        assertThat(entity.isAutoApproveOidc()).isTrue();
        assertThat(entity.getAutoApproveEmailDomains()).containsExactlyInAnyOrder("example.com", "test.org");
        assertThat(entity.getImpressumContent()).isEqualTo("impressum");
        assertThat(entity.getPrivacyPolicyContent()).isEqualTo("privacy");
        assertThat(entity.getOidcIssuerUrl()).isEqualTo("https://issuer");
        assertThat(entity.getOidcClientId()).isEqualTo("cid");
        assertThat(entity.getOidcClientSecret()).isEqualTo("cs");
    }

    @Test
    @DisplayName("SWR-072: toDomain maps JPA entity to domain")
    void toDomainMaps() {
        var entity = new TenantJpaEntity();
        var uuid = java.util.UUID.randomUUID();
        entity.setTenantId(uuid);
        entity.setSlug("blog");
        entity.setDisplayName("Blog");
        entity.setTagline("tag");
        entity.setStatus("SUSPENDED");
        entity.setLoginMode("INTERNAL");
        entity.setAutoApproveOidc(false);
        entity.setAutoApproveEmailDomains(Set.of("corp.com"));
        entity.setImpressumContent("imp");
        entity.setPrivacyPolicyContent("priv");
        entity.setOidcIssuerUrl("https://iss");
        entity.setOidcClientId("c");
        entity.setOidcClientSecret("s");

        var tenant = TenantMapper.toDomain(entity);

        assertThat(tenant.getTenantId().value()).isEqualTo(uuid);
        assertThat(tenant.getSlug()).isEqualTo("blog");
        assertThat(tenant.getDisplayName()).isEqualTo("Blog");
        assertThat(tenant.getTagline()).isEqualTo("tag");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
        assertThat(tenant.getLoginMode()).isEqualTo(LoginMode.INTERNAL);
        assertThat(tenant.isAutoApproveOidc()).isFalse();
        assertThat(tenant.getAutoApproveEmailDomains()).containsExactly("corp.com");
        assertThat(tenant.getImpressumContent()).isEqualTo("imp");
        assertThat(tenant.getPrivacyPolicyContent()).isEqualTo("priv");
        assertThat(tenant.getOidcIssuerUrl()).isEqualTo("https://iss");
        assertThat(tenant.getOidcClientId()).isEqualTo("c");
        assertThat(tenant.getOidcClientSecret()).isEqualTo("s");
    }

    @Test
    @DisplayName("SWR-072: round-trip mapping preserves state")
    void roundTripPreservesState() {
        var tenantId = TenantId.generate();
        var original = Tenant.reconstitute(
                tenantId,
                "slug",
                "Display",
                null,
                TenantStatus.ACTIVE,
                LoginMode.BOTH,
                false,
                Set.of(),
                null,
                null,
                null,
                null,
                null);

        var entity = TenantMapper.toEntity(original);
        var restored = TenantMapper.toDomain(entity);

        assertThat(restored.getTenantId()).isEqualTo(original.getTenantId());
        assertThat(restored.getSlug()).isEqualTo(original.getSlug());
        assertThat(restored.getDisplayName()).isEqualTo(original.getDisplayName());
        assertThat(restored.getStatus()).isEqualTo(original.getStatus());
        assertThat(restored.getLoginMode()).isEqualTo(original.getLoginMode());
    }
}
