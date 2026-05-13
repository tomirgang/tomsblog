package de.tomsblog.usermanagement.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TenantSettingsMapperTest {

    @Test
    @DisplayName("SWR-044: toEntity maps domain to JPA entity")
    void toEntityMaps() {
        var tenantId = TenantId.generate();
        var settings = TenantSettings.reconstitute(
                tenantId,
                LoginMode.OIDC,
                true,
                Set.of("example.com", "test.org"),
                "Toms Blog",
                null,
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
        var original = TenantSettings.reconstitute(
                tenantId,
                LoginMode.BOTH,
                true,
                Set.of("a.com", "b.com"),
                "Toms Blog",
                null,
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

        var entity = TenantSettingsMapper.toEntity(original);
        var restored = TenantSettingsMapper.toDomain(entity);

        assertThat(restored.getTenantId()).isEqualTo(original.getTenantId());
        assertThat(restored.getLoginMode()).isEqualTo(original.getLoginMode());
        assertThat(restored.isAutoApproveOidc()).isEqualTo(original.isAutoApproveOidc());
        assertThat(restored.getAutoApproveEmailDomains()).isEqualTo(original.getAutoApproveEmailDomains());
    }

    @Test
    @DisplayName("SWR-050: toEntity maps branding fields")
    void toEntityMapsBranding() {
        var tenantId = TenantId.generate();
        var settings = TenantSettings.reconstitute(
                tenantId,
                LoginMode.BOTH,
                false,
                Set.of(),
                "My Blog",
                "A tagline",
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

        var entity = TenantSettingsMapper.toEntity(settings);

        assertThat(entity.getDisplayName()).isEqualTo("My Blog");
        assertThat(entity.getTagline()).isEqualTo("A tagline");
    }

    @Test
    @DisplayName("SWR-050: toDomain maps branding fields")
    void toDomainMapsBranding() {
        var entity = new TenantSettingsJpaEntity();
        entity.setTenantId(java.util.UUID.randomUUID());
        entity.setLoginMode("BOTH");
        entity.setAutoApproveOidc(false);
        entity.setAutoApproveEmailDomains(Set.of());
        entity.setDisplayName("Custom Blog");
        entity.setTagline("Great tagline");

        var settings = TenantSettingsMapper.toDomain(entity);

        assertThat(settings.getDisplayName()).isEqualTo("Custom Blog");
        assertThat(settings.getTagline()).isEqualTo("Great tagline");
    }

    @Test
    @DisplayName("SWR-050: toEntity defaults displayName when null")
    void toEntityDefaultsDisplayName() {
        var tenantId = TenantId.generate();
        var settings = TenantSettings.reconstitute(
                tenantId,
                LoginMode.BOTH,
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
                null,
                null,
                false,
                Map.of());

        var entity = TenantSettingsMapper.toEntity(settings);

        assertThat(entity.getDisplayName()).isEqualTo("Toms Blog");
    }

    @Test
    @DisplayName("SWR-050: roundtrip preserves branding fields")
    void roundtripPreservesBranding() {
        var tenantId = TenantId.generate();
        var original = TenantSettings.reconstitute(
                tenantId,
                LoginMode.BOTH,
                false,
                Set.of(),
                "My Blog",
                "My tagline",
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

        var entity = TenantSettingsMapper.toEntity(original);
        var restored = TenantSettingsMapper.toDomain(entity);

        assertThat(restored.getDisplayName()).isEqualTo("My Blog");
        assertThat(restored.getTagline()).isEqualTo("My tagline");
    }

    @Test
    @DisplayName("SWR-092: toEntity maps oidcButtonText")
    void toEntityMapsOidcButtonText() {
        var tenantId = TenantId.generate();
        var settings = TenantSettings.reconstitute(
                tenantId,
                LoginMode.BOTH,
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                null,
                null,
                null,
                "Login with SSO",
                null,
                null,
                null,
                false,
                Map.of());

        var entity = TenantSettingsMapper.toEntity(settings);

        assertThat(entity.getOidcButtonText()).isEqualTo("Login with SSO");
    }

    @Test
    @DisplayName("SWR-094: toEntity maps defaultRole")
    void toEntityMapsDefaultRole() {
        var tenantId = TenantId.generate();
        var settings = TenantSettings.reconstitute(
                tenantId,
                LoginMode.BOTH,
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "AUTHOR",
                null,
                null,
                false,
                Map.of());

        var entity = TenantSettingsMapper.toEntity(settings);

        assertThat(entity.getDefaultRole()).isEqualTo("AUTHOR");
    }

    @Test
    @DisplayName("SWR-094: toEntity defaults defaultRole to READER when null")
    void toEntityDefaultsDefaultRole() {
        var tenantId = TenantId.generate();
        var settings = TenantSettings.reconstitute(
                tenantId,
                LoginMode.BOTH,
                false,
                Set.of(),
                "Blog",
                null,
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

        var entity = TenantSettingsMapper.toEntity(settings);

        assertThat(entity.getDefaultRole()).isEqualTo("READER");
    }

    @Test
    @DisplayName("SWR-091: toEntity maps logoUrl and faviconUrl")
    void toEntityMapsLogoAndFavicon() {
        var tenantId = TenantId.generate();
        var settings = TenantSettings.reconstitute(
                tenantId,
                LoginMode.BOTH,
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "https://example.com/logo.png",
                "https://example.com/favicon.ico",
                false,
                Map.of());

        var entity = TenantSettingsMapper.toEntity(settings);

        assertThat(entity.getLogoUrl()).isEqualTo("https://example.com/logo.png");
        assertThat(entity.getFaviconUrl()).isEqualTo("https://example.com/favicon.ico");
    }

    @Test
    @DisplayName("SWR-095: toEntity maps oidcRoleMappingEnabled and oidcRoleMappings")
    void toEntityMapsRoleMappings() {
        var tenantId = TenantId.generate();
        var settings = TenantSettings.reconstitute(
                tenantId,
                LoginMode.BOTH,
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                Map.of("admins", "ADMIN", "devs", "AUTHOR"));

        var entity = TenantSettingsMapper.toEntity(settings);

        assertThat(entity.isOidcRoleMappingEnabled()).isTrue();
        assertThat(entity.getOidcRoleMappings()).containsEntry("admins", "ADMIN");
        assertThat(entity.getOidcRoleMappings()).containsEntry("devs", "AUTHOR");
    }

    @Test
    @DisplayName("SWR-095: toDomain maps oidcRoleMappingEnabled and oidcRoleMappings")
    void toDomainMapsRoleMappings() {
        var entity = new TenantSettingsJpaEntity();
        entity.setTenantId(java.util.UUID.randomUUID());
        entity.setLoginMode("BOTH");
        entity.setAutoApproveOidc(false);
        entity.setAutoApproveEmailDomains(Set.of());
        entity.setDisplayName("Blog");
        entity.setOidcRoleMappingEnabled(true);
        entity.setOidcRoleMappings(Map.of("ops", "ADMIN"));

        var settings = TenantSettingsMapper.toDomain(entity);

        assertThat(settings.isOidcRoleMappingEnabled()).isTrue();
        assertThat(settings.getOidcRoleMappings()).containsEntry("ops", "ADMIN");
    }

    @Test
    @DisplayName("SWR-091: roundtrip preserves all new fields")
    void roundtripPreservesNewFields() {
        var tenantId = TenantId.generate();
        var original = TenantSettings.reconstitute(
                tenantId,
                LoginMode.BOTH,
                true,
                Set.of("a.com"),
                "My Blog",
                "Tagline",
                "Impressum",
                "Privacy",
                "https://issuer.example.com",
                "client-id",
                "client-secret",
                "Login with OIDC",
                "AUTHOR",
                "https://logo.png",
                "https://favicon.ico",
                true,
                Map.of("admins", "ADMIN"));

        var entity = TenantSettingsMapper.toEntity(original);
        var restored = TenantSettingsMapper.toDomain(entity);

        assertThat(restored.getOidcButtonText()).isEqualTo("Login with OIDC");
        assertThat(restored.getDefaultRole()).isEqualTo("AUTHOR");
        assertThat(restored.getLogoUrl()).isEqualTo("https://logo.png");
        assertThat(restored.getFaviconUrl()).isEqualTo("https://favicon.ico");
        assertThat(restored.isOidcRoleMappingEnabled()).isTrue();
        assertThat(restored.getOidcRoleMappings()).containsEntry("admins", "ADMIN");
        assertThat(restored.getOidcIssuerUrl()).isEqualTo("https://issuer.example.com");
        assertThat(restored.getOidcClientId()).isEqualTo("client-id");
        assertThat(restored.getOidcClientSecret()).isEqualTo("client-secret");
    }
}
