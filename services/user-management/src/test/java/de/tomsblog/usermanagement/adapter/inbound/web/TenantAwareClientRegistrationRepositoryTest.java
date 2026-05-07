package de.tomsblog.usermanagement.adapter.inbound.web;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import java.util.Set;
import java.util.UUID;
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
                tenantSettingsUseCase, fallbackRegistration, DEFAULT_TENANT);
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
                "tenant-secret");
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        ClientRegistration reg = repository.findByRegistrationId("authentik");

        assertThat(reg.getClientId()).isEqualTo("tenant-client-id");
        assertThat(reg.getClientSecret()).isEqualTo("tenant-secret");
        assertThat(reg.getProviderDetails().getAuthorizationUri()).isEqualTo("https://auth.tenant.com/authorize/");
    }

    @Test
    @DisplayName("returns fallback when no OIDC config in tenant settings")
    void returnsFallbackWhenNoOidc() {
        var settings = TenantSettings.reconstitute(
                TENANT_ID, LoginMode.BOTH, false, Set.of(), "Blog", null, null, null, null, null, null);
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
                "tenant-secret");
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
                null);
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        ClientRegistration reg = repository.findByRegistrationId("authentik");

        assertThat(reg.getProviderDetails().getTokenUri()).isEqualTo("https://auth.tenant.com/token/");
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
                "secret");
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        ClientRegistration reg = repository.findByRegistrationId("authentik");

        assertThat(reg.getProviderDetails().getTokenUri()).isEqualTo("https://auth.tenant.com/token/");
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
                "secret");
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        ClientRegistration reg = repository.findByRegistrationId("authentik");

        assertThat(reg.getClientId()).isEqualTo("fallback-client");
    }
}
