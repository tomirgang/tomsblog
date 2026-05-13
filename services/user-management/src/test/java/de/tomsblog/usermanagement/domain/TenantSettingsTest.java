package de.tomsblog.usermanagement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.domain.model.AuthSource;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TenantSettingsTest {

    private static final TenantId TENANT_ID = TenantId.generate();

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("SWR-044: create returns defaults (BOTH, no auto-approval)")
        void createReturnsDefaults() {
            var settings = TenantSettings.create(TENANT_ID);

            assertThat(settings.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(settings.getLoginMode()).isEqualTo(LoginMode.INTERNAL);
            assertThat(settings.isAutoApproveOidc()).isFalse();
            assertThat(settings.getAutoApproveEmailDomains()).isEmpty();
        }

        @Test
        @DisplayName("SWR-044: create with null tenantId throws")
        void createWithNullTenantIdThrows() {
            assertThatNullPointerException().isThrownBy(() -> TenantSettings.create(null));
        }

        @Test
        @DisplayName("SWR-044: reconstitute recreates settings from persistence")
        void reconstituteRecreatesSettings() {
            var settings = TenantSettings.reconstitute(
                    TENANT_ID,
                    LoginMode.OIDC,
                    true,
                    Set.of("example.com"),
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

            assertThat(settings.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(settings.getLoginMode()).isEqualTo(LoginMode.OIDC);
            assertThat(settings.isAutoApproveOidc()).isTrue();
            assertThat(settings.getAutoApproveEmailDomains()).containsExactly("example.com");
        }
    }

    @Nested
    @DisplayName("Login mode")
    class LoginModeTests {

        @Test
        @DisplayName("SWR-044: updateLoginMode changes mode")
        void updateLoginModeChangesMode() {
            var settings = TenantSettings.create(TENANT_ID);

            settings.updateLoginMode(LoginMode.OIDC);

            assertThat(settings.getLoginMode()).isEqualTo(LoginMode.OIDC);
        }

        @Test
        @DisplayName("SWR-044: updateLoginMode with null throws")
        void updateLoginModeWithNullThrows() {
            var settings = TenantSettings.create(TENANT_ID);

            assertThatNullPointerException().isThrownBy(() -> settings.updateLoginMode(null));
        }
    }

    @Nested
    @DisplayName("Auto-approval")
    class AutoApproval {

        @Test
        @DisplayName("SWR-045: updateAutoApproveOidc enables auto-approval")
        void updateAutoApproveOidc() {
            var settings = TenantSettings.create(TENANT_ID);

            settings.updateAutoApproveOidc(true);

            assertThat(settings.isAutoApproveOidc()).isTrue();
        }

        @Test
        @DisplayName("SWR-045: addAutoApproveEmailDomain adds domain lowercase")
        void addAutoApproveEmailDomain() {
            var settings = TenantSettings.create(TENANT_ID);

            settings.addAutoApproveEmailDomain("Example.COM");

            assertThat(settings.getAutoApproveEmailDomains()).containsExactly("example.com");
        }

        @Test
        @DisplayName("SWR-045: addAutoApproveEmailDomain with null throws")
        void addAutoApproveEmailDomainNullThrows() {
            var settings = TenantSettings.create(TENANT_ID);

            assertThatNullPointerException().isThrownBy(() -> settings.addAutoApproveEmailDomain(null));
        }

        @Test
        @DisplayName("SWR-045: removeAutoApproveEmailDomain removes domain")
        void removeAutoApproveEmailDomain() {
            var settings = TenantSettings.create(TENANT_ID);
            settings.addAutoApproveEmailDomain("example.com");

            settings.removeAutoApproveEmailDomain("example.com");

            assertThat(settings.getAutoApproveEmailDomains()).isEmpty();
        }

        @Test
        @DisplayName("SWR-045: removeAutoApproveEmailDomain with null throws")
        void removeAutoApproveEmailDomainNullThrows() {
            var settings = TenantSettings.create(TENANT_ID);

            assertThatNullPointerException().isThrownBy(() -> settings.removeAutoApproveEmailDomain(null));
        }

        @Test
        @DisplayName("SWR-045: setAutoApproveEmailDomains replaces all domains")
        void setAutoApproveEmailDomains() {
            var settings = TenantSettings.create(TENANT_ID);
            settings.addAutoApproveEmailDomain("old.com");

            settings.setAutoApproveEmailDomains(Set.of("New.COM", "other.org"));

            assertThat(settings.getAutoApproveEmailDomains()).containsExactlyInAnyOrder("new.com", "other.org");
        }

        @Test
        @DisplayName("SWR-045: setAutoApproveEmailDomains with null throws")
        void setAutoApproveEmailDomainsNullThrows() {
            var settings = TenantSettings.create(TENANT_ID);

            assertThatNullPointerException().isThrownBy(() -> settings.setAutoApproveEmailDomains(null));
        }
    }

    @Nested
    @DisplayName("shouldAutoApprove")
    class ShouldAutoApprove {

        @Test
        @DisplayName("SWR-045: approves OIDC user when autoApproveOidc enabled")
        void approvesOidcUserWhenEnabled() {
            var settings = TenantSettings.create(TENANT_ID);
            settings.updateAutoApproveOidc(true);

            assertThat(settings.shouldAutoApprove(AuthSource.OIDC, "user@any.com"))
                    .isTrue();
        }

        @Test
        @DisplayName("SWR-045: does not approve OIDC user when autoApproveOidc disabled")
        void doesNotApproveOidcWhenDisabled() {
            var settings = TenantSettings.create(TENANT_ID);

            assertThat(settings.shouldAutoApprove(AuthSource.OIDC, "user@any.com"))
                    .isFalse();
        }

        @Test
        @DisplayName("SWR-045: approves user with matching email domain")
        void approvesMatchingEmailDomain() {
            var settings = TenantSettings.create(TENANT_ID);
            settings.addAutoApproveEmailDomain("company.com");

            assertThat(settings.shouldAutoApprove(AuthSource.INTERNAL, "user@company.com"))
                    .isTrue();
        }

        @Test
        @DisplayName("SWR-045: does not approve user with non-matching email domain")
        void doesNotApproveNonMatchingDomain() {
            var settings = TenantSettings.create(TENANT_ID);
            settings.addAutoApproveEmailDomain("company.com");

            assertThat(settings.shouldAutoApprove(AuthSource.INTERNAL, "user@other.com"))
                    .isFalse();
        }

        @Test
        @DisplayName("SWR-045: does not approve when email is null")
        void doesNotApproveNullEmail() {
            var settings = TenantSettings.create(TENANT_ID);
            settings.addAutoApproveEmailDomain("company.com");

            assertThat(settings.shouldAutoApprove(AuthSource.INTERNAL, null)).isFalse();
        }

        @Test
        @DisplayName("SWR-045: handles email without valid domain")
        void handlesInvalidEmail() {
            var settings = TenantSettings.create(TENANT_ID);
            settings.addAutoApproveEmailDomain("company.com");

            assertThat(settings.shouldAutoApprove(AuthSource.INTERNAL, "nodomain@"))
                    .isFalse();
        }

        @Test
        @DisplayName("SWR-045: handles email without @ symbol")
        void handlesEmailWithoutAtSymbol() {
            var settings = TenantSettings.create(TENANT_ID);
            settings.addAutoApproveEmailDomain("company.com");

            assertThat(settings.shouldAutoApprove(AuthSource.INTERNAL, "noatsymbol"))
                    .isFalse();
        }

        @Test
        @DisplayName("SWR-045: email domain matching is case-insensitive")
        void emailDomainMatchingCaseInsensitive() {
            var settings = TenantSettings.create(TENANT_ID);
            settings.addAutoApproveEmailDomain("company.com");

            assertThat(settings.shouldAutoApprove(AuthSource.INTERNAL, "user@COMPANY.COM"))
                    .isTrue();
        }

        @Test
        @DisplayName("SWR-045: returns false when no rules configured")
        void returnsFalseWhenNoRules() {
            var settings = TenantSettings.create(TENANT_ID);

            assertThat(settings.shouldAutoApprove(AuthSource.OIDC, "user@any.com"))
                    .isFalse();
            assertThat(settings.shouldAutoApprove(AuthSource.INTERNAL, "user@any.com"))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("Branding")
    class Branding {

        @Test
        @DisplayName("SWR-050: create sets default displayName")
        void createSetsDefaultDisplayName() {
            var settings = TenantSettings.create(TENANT_ID);

            assertThat(settings.getDisplayName()).isEqualTo("Toms Blog");
            assertThat(settings.getTagline()).isNull();
        }

        @Test
        @DisplayName("SWR-050: reconstitute preserves displayName and tagline")
        void reconstitutePreservesBranding() {
            var settings = TenantSettings.reconstitute(
                    TENANT_ID,
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

            assertThat(settings.getDisplayName()).isEqualTo("My Blog");
            assertThat(settings.getTagline()).isEqualTo("A tagline");
        }

        @Test
        @DisplayName("SWR-050: updateDisplayName changes display name")
        void updateDisplayName() {
            var settings = TenantSettings.create(TENANT_ID);

            settings.updateDisplayName("New Name");

            assertThat(settings.getDisplayName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("SWR-050: updateTagline changes tagline")
        void updateTagline() {
            var settings = TenantSettings.create(TENANT_ID);

            settings.updateTagline("My tagline");

            assertThat(settings.getTagline()).isEqualTo("My tagline");
        }

        @Test
        @DisplayName("SWR-050: updateTagline allows null")
        void updateTaglineAllowsNull() {
            var settings = TenantSettings.reconstitute(
                    TENANT_ID,
                    LoginMode.BOTH,
                    false,
                    Set.of(),
                    "Blog",
                    "old tagline",
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

            settings.updateTagline(null);

            assertThat(settings.getTagline()).isNull();
        }
    }
}
