package de.tomsblog.tenantmanagement.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tomsblog.shared.tenant.TenantId;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TenantTest {

    @Test
    @DisplayName("SWR-072: create sets default values")
    void createSetsDefaults() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.create(tenantId, "my-blog", "My Blog");

        assertThat(tenant.getTenantId()).isEqualTo(tenantId);
        assertThat(tenant.getSlug()).isEqualTo("my-blog");
        assertThat(tenant.getDisplayName()).isEqualTo("My Blog");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(tenant.getLoginMode()).isEqualTo(LoginMode.INTERNAL);
        assertThat(tenant.isAutoApproveOidc()).isFalse();
        assertThat(tenant.getAutoApproveEmailDomains()).isEmpty();
    }

    @Test
    @DisplayName("SWR-072: create rejects null tenantId")
    void createRejectsNullTenantId() {
        assertThatThrownBy(() -> Tenant.create(null, "slug", "name")).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("SWR-072: create rejects null slug")
    void createRejectsNullSlug() {
        assertThatThrownBy(() -> Tenant.create(TenantId.generate(), null, "name"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("SWR-072: create rejects null displayName")
    void createRejectsNullDisplayName() {
        assertThatThrownBy(() -> Tenant.create(TenantId.generate(), "slug", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("SWR-072: reconstitute restores full state")
    void reconstituteRestoresState() {
        var tenantId = TenantId.generate();
        var tenant = Tenant.reconstitute(
                tenantId,
                "blog",
                "Blog Name",
                "A tagline",
                TenantStatus.SUSPENDED,
                LoginMode.OIDC,
                true,
                Set.of("example.com"),
                "impressum",
                "privacy",
                "https://auth.example.com",
                "client-id",
                "client-secret",
                null,
                "READER",
                null,
                null,
                false,
                Map.of());

        assertThat(tenant.getSlug()).isEqualTo("blog");
        assertThat(tenant.getTagline()).isEqualTo("A tagline");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
        assertThat(tenant.getLoginMode()).isEqualTo(LoginMode.OIDC);
        assertThat(tenant.isAutoApproveOidc()).isTrue();
        assertThat(tenant.getAutoApproveEmailDomains()).containsExactly("example.com");
        assertThat(tenant.getImpressumContent()).isEqualTo("impressum");
        assertThat(tenant.getPrivacyPolicyContent()).isEqualTo("privacy");
        assertThat(tenant.getOidcIssuerUrl()).isEqualTo("https://auth.example.com");
        assertThat(tenant.getOidcClientId()).isEqualTo("client-id");
        assertThat(tenant.getOidcClientSecret()).isEqualTo("client-secret");
    }

    @Test
    @DisplayName("SWR-072: updateGeneralSettings changes branding and login")
    void updateGeneralSettingsWorks() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        tenant.updateGeneralSettings("New Name", "New Tag", LoginMode.OIDC, true, Set.of("new.com"));

        assertThat(tenant.getDisplayName()).isEqualTo("New Name");
        assertThat(tenant.getTagline()).isEqualTo("New Tag");
        assertThat(tenant.getLoginMode()).isEqualTo(LoginMode.OIDC);
        assertThat(tenant.isAutoApproveOidc()).isTrue();
        assertThat(tenant.getAutoApproveEmailDomains()).containsExactly("new.com");
    }

    @Test
    @DisplayName("SWR-072: updateOidcSettings changes OIDC config")
    void updateOidcSettingsWorks() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        tenant.updateOidcSettings("https://issuer", "cid", "csecret", null, false, Map.of());

        assertThat(tenant.getOidcIssuerUrl()).isEqualTo("https://issuer");
        assertThat(tenant.getOidcClientId()).isEqualTo("cid");
        assertThat(tenant.getOidcClientSecret()).isEqualTo("csecret");
    }

    @Test
    @DisplayName("SWR-072: updateOidcSettings preserves secret when masked")
    void updateOidcSettingsPreservesSecret() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        tenant.updateOidcSettings("https://issuer", "cid", "original", null, false, Map.of());
        tenant.updateOidcSettings("https://issuer", "cid", "***", null, false, Map.of());

        assertThat(tenant.getOidcClientSecret()).isEqualTo("original");
    }

    @Test
    @DisplayName("SWR-072: updateOidcSettings preserves secret when empty")
    void updateOidcSettingsPreservesSecretEmpty() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        tenant.updateOidcSettings("https://issuer", "cid", "original", null, false, Map.of());
        tenant.updateOidcSettings("https://issuer", "cid", "", null, false, Map.of());

        assertThat(tenant.getOidcClientSecret()).isEqualTo("original");
    }

    @Test
    @DisplayName("SWR-072: updateOidcSettings preserves secret when null")
    void updateOidcSettingsPreservesSecretNull() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        tenant.updateOidcSettings("https://issuer", "cid", "original", null, false, Map.of());
        tenant.updateOidcSettings("https://issuer", "cid", null, null, false, Map.of());

        assertThat(tenant.getOidcClientSecret()).isEqualTo("original");
    }

    @Test
    @DisplayName("SWR-072: updateLegalSettings changes legal content")
    void updateLegalSettingsWorks() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        tenant.updateLegalSettings("imp", "priv");

        assertThat(tenant.getImpressumContent()).isEqualTo("imp");
        assertThat(tenant.getPrivacyPolicyContent()).isEqualTo("priv");
    }

    @Test
    @DisplayName("SWR-072: lifecycle transitions work")
    void lifecycleTransitions() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);

        tenant.suspend();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.SUSPENDED);

        tenant.activate();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);

        tenant.deactivate();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.DEACTIVATED);
    }

    @Test
    @DisplayName("SWR-072: shouldAutoApprove with OIDC auto-approve")
    void shouldAutoApproveOidc() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        tenant.updateGeneralSettings("Name", null, LoginMode.OIDC, true, Set.of());

        assertThat(tenant.shouldAutoApprove("OIDC", "user@any.com")).isTrue();
        assertThat(tenant.shouldAutoApprove("INTERNAL", "user@any.com")).isFalse();
    }

    @Test
    @DisplayName("SWR-072: shouldAutoApprove returns false for OIDC when not enabled")
    void shouldAutoApproveOidcNotEnabled() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        tenant.updateGeneralSettings("Name", null, LoginMode.OIDC, false, Set.of());

        assertThat(tenant.shouldAutoApprove("OIDC", "user@any.com")).isFalse();
    }

    @Test
    @DisplayName("SWR-072: shouldAutoApprove with email domain")
    void shouldAutoApproveEmailDomain() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        tenant.updateGeneralSettings("Name", null, LoginMode.BOTH, false, Set.of("example.com"));

        assertThat(tenant.shouldAutoApprove("INTERNAL", "user@example.com")).isTrue();
        assertThat(tenant.shouldAutoApprove("INTERNAL", "user@other.com")).isFalse();
    }

    @Test
    @DisplayName("SWR-072: shouldAutoApprove returns false for invalid email")
    void shouldAutoApproveInvalidEmail() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        tenant.updateGeneralSettings("Name", null, LoginMode.BOTH, false, Set.of("example.com"));

        assertThat(tenant.shouldAutoApprove("INTERNAL", "noemail")).isFalse();
        assertThat(tenant.shouldAutoApprove("INTERNAL", null)).isFalse();
    }

    @Test
    @DisplayName("SWR-072: shouldAutoApprove returns false for email ending in @")
    void shouldAutoApproveEmailEndingInAt() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        tenant.updateGeneralSettings("Name", null, LoginMode.BOTH, false, Set.of("example.com"));

        assertThat(tenant.shouldAutoApprove("INTERNAL", "user@")).isFalse();
    }

    @Test
    @DisplayName("SWR-072: autoApproveEmailDomains are lowercased")
    void autoApproveEmailDomainsLowercased() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        tenant.updateGeneralSettings("Name", null, LoginMode.BOTH, false, Set.of("Example.COM"));

        assertThat(tenant.getAutoApproveEmailDomains()).containsExactly("example.com");
    }

    @Test
    @DisplayName("SWR-072: autoApproveEmailDomains returns unmodifiable set")
    void autoApproveEmailDomainsUnmodifiable() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        assertThatThrownBy(() -> tenant.getAutoApproveEmailDomains().add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("SWR-091: create sets default logoUrl and faviconUrl to null")
    void createSetsDefaultLogoAndFavicon() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");

        assertThat(tenant.getLogoUrl()).isNull();
        assertThat(tenant.getFaviconUrl()).isNull();
    }

    @Test
    @DisplayName("SWR-091: updateLogoUrl and updateFaviconUrl set values")
    void updateLogoAndFaviconUrls() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        tenant.updateLogoUrl("https://logo.png");
        tenant.updateFaviconUrl("https://fav.ico");

        assertThat(tenant.getLogoUrl()).isEqualTo("https://logo.png");
        assertThat(tenant.getFaviconUrl()).isEqualTo("https://fav.ico");
    }

    @Test
    @DisplayName("SWR-092: updateOidcSettings sets oidcButtonText")
    void updateOidcSettingsButtonText() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        tenant.updateOidcSettings("https://iss", "cid", "cs", "Login with SSO", false, Map.of());

        assertThat(tenant.getOidcButtonText()).isEqualTo("Login with SSO");
    }

    @Test
    @DisplayName("SWR-094: create sets default role to READER")
    void createSetsDefaultRole() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");

        assertThat(tenant.getDefaultRole()).isEqualTo("READER");
    }

    @Test
    @DisplayName("SWR-094: updateDefaultRole changes role")
    void updateDefaultRole() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        tenant.updateDefaultRole("AUTHOR");

        assertThat(tenant.getDefaultRole()).isEqualTo("AUTHOR");
    }

    @Test
    @DisplayName("SWR-095: updateOidcSettings sets role mappings")
    void updateOidcSettingsRoleMappings() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        tenant.updateOidcSettings(
                "https://iss", "cid", "cs", null, true, Map.of("admins", "ADMIN", "editors", "AUTHOR"));

        assertThat(tenant.isOidcRoleMappingEnabled()).isTrue();
        assertThat(tenant.getOidcRoleMappings()).containsEntry("admins", "ADMIN");
        assertThat(tenant.getOidcRoleMappings()).containsEntry("editors", "AUTHOR");
    }

    @Test
    @DisplayName("SWR-095: updateOidcSettings clears old role mappings before setting new ones")
    void updateOidcSettingsClearsOldRoleMappings() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        tenant.updateOidcSettings("https://iss", "cid", "cs", null, true, Map.of("old", "OLD"));
        tenant.updateOidcSettings("https://iss", "cid", "cs", null, true, Map.of("new", "NEW"));

        assertThat(tenant.getOidcRoleMappings()).containsEntry("new", "NEW");
        assertThat(tenant.getOidcRoleMappings()).doesNotContainKey("old");
    }

    @Test
    @DisplayName("SWR-095: updateOidcSettings handles null role mappings")
    void updateOidcSettingsNullRoleMappings() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        tenant.updateOidcSettings("https://iss", "cid", "cs", null, false, null);

        assertThat(tenant.getOidcRoleMappings()).isEmpty();
    }

    @Test
    @DisplayName("SWR-095: reconstitute handles null oidcRoleMappings")
    void reconstituteNullRoleMappings() {
        var tenant = Tenant.reconstitute(
                TenantId.generate(),
                "s",
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
                "READER",
                null,
                null,
                false,
                null);

        assertThat(tenant.getOidcRoleMappings()).isEmpty();
    }

    @Test
    @DisplayName("SWR-095: oidcRoleMappings returns unmodifiable map")
    void oidcRoleMappingsUnmodifiable() {
        var tenant = Tenant.create(TenantId.generate(), "s", "Name");
        assertThatThrownBy(() -> tenant.getOidcRoleMappings().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("SWR-092: reconstitute restores oidcButtonText, defaultRole, logoUrl, faviconUrl, roleMappings")
    void reconstituteNewFields() {
        var tenant = Tenant.reconstitute(
                TenantId.generate(),
                "s",
                "Name",
                null,
                TenantStatus.ACTIVE,
                LoginMode.OIDC,
                false,
                Set.of(),
                null,
                null,
                "https://iss",
                "cid",
                "cs",
                "Login with SSO",
                "AUTHOR",
                "https://logo.png",
                "https://fav.ico",
                true,
                Map.of("admins", "ADMIN"));

        assertThat(tenant.getOidcButtonText()).isEqualTo("Login with SSO");
        assertThat(tenant.getDefaultRole()).isEqualTo("AUTHOR");
        assertThat(tenant.getLogoUrl()).isEqualTo("https://logo.png");
        assertThat(tenant.getFaviconUrl()).isEqualTo("https://fav.ico");
        assertThat(tenant.isOidcRoleMappingEnabled()).isTrue();
        assertThat(tenant.getOidcRoleMappings()).containsEntry("admins", "ADMIN");
    }
}
