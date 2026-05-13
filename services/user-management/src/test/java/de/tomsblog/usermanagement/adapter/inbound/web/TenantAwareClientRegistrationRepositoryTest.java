package de.tomsblog.usermanagement.adapter.inbound.web;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

@DisplayName("SWR-063: TenantAwareClientRegistrationRepository")
class TenantAwareClientRegistrationRepositoryTest {

    private static final String DEFAULT_TENANT = "00000000-0000-0000-0000-000000000001";
    private static final TenantId TENANT_ID = TenantId.of(UUID.fromString(DEFAULT_TENANT));

    private TenantSettingsUseCase tenantSettingsUseCase;
    private ClientRegistration fallbackRegistration;
    private TenantAwareClientRegistrationRepository repository;

    /**
     * Mock issuer resolver that simulates OIDC Discovery by returning a builder
     * with endpoints derived from the issuer URL (similar to what a real
     * {@code .well-known/openid-configuration} response would provide).
     */
    private static final Function<String, ClientRegistration.Builder> MOCK_ISSUER_RESOLVER =
            issuerUrl -> ClientRegistration.withRegistrationId("discovered")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .authorizationUri(issuerUrl + "/authorize")
                    .tokenUri(issuerUrl + "/token")
                    .userInfoUri(issuerUrl + "/userinfo")
                    .jwkSetUri(issuerUrl + "/jwks")
                    .clientId("placeholder");

    @BeforeEach
    void setUp() {
        tenantSettingsUseCase = mock(TenantSettingsUseCase.class);
        fallbackRegistration = ClientRegistration.withRegistrationId("authentik")
                .clientId("fallback-client")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://fallback.example.com/authorize")
                .tokenUri("https://fallback.example.com/token")
                .build();
        repository = new TenantAwareClientRegistrationRepository(
                tenantSettingsUseCase, fallbackRegistration, DEFAULT_TENANT, MOCK_ISSUER_RESOLVER);
    }

    @Test
    @DisplayName("returns tenant-specific registration when OIDC config exists")
    void returnsTenantRegistration() {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
                LoginMode.OIDC,
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                "https://auth.tenant.com",
                "tenant-client-id",
                "tenant-secret",
                null,
                null,
                null,
                null,
                false,
                Map.of());
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        ClientRegistration reg = repository.findByRegistrationId("authentik");

        assertThat(reg.getClientId()).isEqualTo("tenant-client-id");
        assertThat(reg.getClientSecret()).isEqualTo("tenant-secret");
        assertThat(reg.getProviderDetails().getAuthorizationUri()).isEqualTo("https://auth.tenant.com/authorize");
    }

    @Test
    @DisplayName("returns fallback when no OIDC config in tenant settings")
    void returnsFallbackWhenNoOidc() {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
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
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        ClientRegistration reg = repository.findByRegistrationId("authentik");

        assertThat(reg.getClientId()).isEqualTo("fallback-client");
    }

    @Test
    @DisplayName("returns fallback on exception")
    void returnsFallbackOnException() {
        when(tenantSettingsUseCase.getSettings(any())).thenThrow(new RuntimeException("DB error"));

        ClientRegistration reg = repository.findByRegistrationId("authentik");

        assertThat(reg.getClientId()).isEqualTo("fallback-client");
    }

    @Test
    @DisplayName("returns fallback when settings are null")
    void returnsFallbackWhenNull() {
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(null);

        ClientRegistration reg = repository.findByRegistrationId("authentik");

        assertThat(reg.getClientId()).isEqualTo("fallback-client");
    }

    @Test
    @DisplayName("caches tenant registration")
    void cachesRegistration() {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
                LoginMode.OIDC,
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                "https://auth.tenant.com",
                "tenant-client-id",
                "tenant-secret",
                null,
                null,
                null,
                null,
                false,
                Map.of());
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        repository.findByRegistrationId("authentik");
        repository.findByRegistrationId("authentik");

        verify(tenantSettingsUseCase, times(1)).getSettings(any());
    }

    @Test
    @DisplayName("handles OIDC issuer URL without trailing slash")
    void issuerWithoutTrailingSlash() {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
                LoginMode.OIDC,
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                "https://auth.tenant.com",
                "client-id",
                null,
                null,
                null,
                null,
                null,
                false,
                Map.of());
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        ClientRegistration reg = repository.findByRegistrationId("authentik");

        assertThat(reg.getProviderDetails().getTokenUri()).isEqualTo("https://auth.tenant.com/token");
        assertThat(reg.getClientSecret()).isEmpty();
    }

    @Test
    @DisplayName("handles OIDC issuer URL with trailing slash")
    void issuerWithTrailingSlash() {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
                LoginMode.OIDC,
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                "https://auth.tenant.com/",
                "client-id",
                "secret",
                null,
                null,
                null,
                null,
                false,
                Map.of());
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        ClientRegistration reg = repository.findByRegistrationId("authentik");

        assertThat(reg.getProviderDetails().getTokenUri()).isEqualTo("https://auth.tenant.com/token");
    }

    @Test
    @DisplayName("returns fallback when issuerUrl present but clientId is null")
    void returnsFallbackWhenClientIdNull() {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
                LoginMode.OIDC,
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                "https://auth.tenant.com",
                null,
                "secret",
                null,
                null,
                null,
                null,
                false,
                Map.of());
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        ClientRegistration reg = repository.findByRegistrationId("authentik");

        assertThat(reg.getClientId()).isEqualTo("fallback-client");
    }

    @Test
    @DisplayName("rejects OIDC issuer with non-HTTP scheme")
    void rejectsNonHttpScheme() {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
                LoginMode.OIDC,
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                "ftp://evil.example.com",
                "client-id",
                "secret",
                null,
                null,
                null,
                null,
                false,
                Map.of());
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        assertThatThrownBy(() -> repository.findByRegistrationId("authentik"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTP(S) scheme");
    }

    @Test
    @DisplayName("rejects OIDC issuer pointing to localhost")
    void rejectsLocalhostIssuer() {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
                LoginMode.OIDC,
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                "https://localhost/auth",
                "client-id",
                "secret",
                null,
                null,
                null,
                null,
                false,
                Map.of());
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        assertThatThrownBy(() -> repository.findByRegistrationId("authentik"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("internal services");
    }

    @Test
    @DisplayName("rejects OIDC issuer pointing to internal Kubernetes service")
    void rejectsInternalK8sService() {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
                LoginMode.OIDC,
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                "https://auth.default.svc.cluster.local",
                "client-id",
                "secret",
                null,
                null,
                null,
                null,
                false,
                Map.of());
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        assertThatThrownBy(() -> repository.findByRegistrationId("authentik"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("internal services");
    }

    @Test
    @DisplayName("rejects OIDC issuer pointing to private IP range 10.x")
    void rejectsPrivateIp10() {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
                LoginMode.OIDC,
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                "https://10.0.0.1/auth",
                "client-id",
                "secret",
                null,
                null,
                null,
                null,
                false,
                Map.of());
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        assertThatThrownBy(() -> repository.findByRegistrationId("authentik"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("internal services");
    }

    @Test
    @DisplayName("rejects OIDC issuer pointing to private IP range 192.168.x")
    void rejectsPrivateIp192() {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
                LoginMode.OIDC,
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                "https://192.168.1.1/auth",
                "client-id",
                "secret",
                null,
                null,
                null,
                null,
                false,
                Map.of());
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        assertThatThrownBy(() -> repository.findByRegistrationId("authentik"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("internal services");
    }

    @Test
    @DisplayName("rejects OIDC issuer pointing to private IP range 172.16.x")
    void rejectsPrivateIp172() {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
                LoginMode.OIDC,
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                "https://172.16.0.1/auth",
                "client-id",
                "secret",
                null,
                null,
                null,
                null,
                false,
                Map.of());
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        assertThatThrownBy(() -> repository.findByRegistrationId("authentik"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("internal services");
    }

    @Test
    @DisplayName("rejects OIDC issuer pointing to loopback 127.x")
    void rejectsLoopbackIp() {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
                LoginMode.OIDC,
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                "https://127.0.0.1/auth",
                "client-id",
                "secret",
                null,
                null,
                null,
                null,
                false,
                Map.of());
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        assertThatThrownBy(() -> repository.findByRegistrationId("authentik"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("internal services");
    }

    @Test
    @DisplayName("rejects OIDC issuer URL without host")
    void rejectsIssuerWithoutHost() {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
                LoginMode.OIDC,
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                "http:///path-only",
                "client-id",
                "secret",
                null,
                null,
                null,
                null,
                false,
                Map.of());
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        assertThatThrownBy(() -> repository.findByRegistrationId("authentik"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must have a host");
    }

    @Test
    @DisplayName("uses cached registration when still valid")
    void usesCacheHit() {
        var settings = TenantSettings.reconstitute(
                TENANT_ID,
                LoginMode.OIDC,
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                "https://auth.tenant.com",
                "tenant-client-id",
                "tenant-secret",
                null,
                null,
                null,
                null,
                false,
                Map.of());
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        // First call populates cache
        ClientRegistration reg1 = repository.findByRegistrationId("authentik");
        // Second call should use cache
        ClientRegistration reg2 = repository.findByRegistrationId("authentik");

        assertThat(reg1.getClientId()).isEqualTo("tenant-client-id");
        assertThat(reg2.getClientId()).isEqualTo("tenant-client-id");
        verify(tenantSettingsUseCase, times(1)).getSettings(any());
    }
}
