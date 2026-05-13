package de.tomsblog.tenantmanagement.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.tenantmanagement.domain.model.LoginMode;
import de.tomsblog.tenantmanagement.domain.model.Tenant;
import de.tomsblog.tenantmanagement.domain.model.TenantStatus;
import java.util.Map;
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
                "cs",
                "Login with SSO",
                "AUTHOR",
                "https://logo.png",
                "https://fav.ico",
                true,
                Map.of("admins", "ADMIN"));

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
        assertThat(entity.getOidcButtonText()).isEqualTo("Login with SSO");
        assertThat(entity.getDefaultRole()).isEqualTo("AUTHOR");
        assertThat(entity.getLogoUrl()).isEqualTo("https://logo.png");
        assertThat(entity.getFaviconUrl()).isEqualTo("https://fav.ico");
        assertThat(entity.isOidcRoleMappingEnabled()).isTrue();
        assertThat(entity.getOidcRoleMappings()).containsEntry("admins", "ADMIN");
    }

    @Test
    @DisplayName("SWR-094: toEntity defaults null defaultRole to READER")
    void toEntityDefaultsNullDefaultRole() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.reconstitute(
                tenantId,
                "slug",
                "Name",
                null,
                TenantStatus.ACTIVE,
                LoginMode.INTERNAL,
                false,
                Set.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                Map.of());

        var entity = TenantMapper.toEntity(tenant);

        assertThat(entity.getDefaultRole()).isEqualTo("READER");
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
        entity.setOidcButtonText("Login via SSO");
        entity.setDefaultRole("AUTHOR");
        entity.setLogoUrl("https://logo.png");
        entity.setFaviconUrl("https://fav.ico");
        entity.setOidcRoleMappingEnabled(true);
        entity.setOidcRoleMappings(Map.of("admins", "ADMIN"));

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
        assertThat(tenant.getOidcButtonText()).isEqualTo("Login via SSO");
        assertThat(tenant.getDefaultRole()).isEqualTo("AUTHOR");
        assertThat(tenant.getLogoUrl()).isEqualTo("https://logo.png");
        assertThat(tenant.getFaviconUrl()).isEqualTo("https://fav.ico");
        assertThat(tenant.isOidcRoleMappingEnabled()).isTrue();
        assertThat(tenant.getOidcRoleMappings()).containsEntry("admins", "ADMIN");
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
                null,
                null,
                "READER",
                null,
                null,
                false,
                Map.of());

        var entity = TenantMapper.toEntity(original);
        var restored = TenantMapper.toDomain(entity);

        assertThat(restored.getTenantId()).isEqualTo(original.getTenantId());
        assertThat(restored.getSlug()).isEqualTo(original.getSlug());
        assertThat(restored.getDisplayName()).isEqualTo(original.getDisplayName());
        assertThat(restored.getStatus()).isEqualTo(original.getStatus());
        assertThat(restored.getLoginMode()).isEqualTo(original.getLoginMode());
    }
}
