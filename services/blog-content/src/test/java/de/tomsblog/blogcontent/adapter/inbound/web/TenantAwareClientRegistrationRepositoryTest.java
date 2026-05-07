package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.TenantSettingsDto;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * @req SWR-063
 */
class TenantAwareClientRegistrationRepositoryTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private UserManagementClient userManagementClient;
    private ClientRegistration fallback;
    private TenantAwareClientRegistrationRepository repository;

    @BeforeEach
    void setUp() {
        userManagementClient = mock(UserManagementClient.class);
        fallback = ClientRegistration.withRegistrationId("authentik")
                .clientId("fallback-client")
                .clientSecret("fallback-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://fallback.example.com/authorize")
                .tokenUri("https://fallback.example.com/token")
                .userInfoUri("https://fallback.example.com/userinfo")
                .jwkSetUri("https://fallback.example.com/jwks")
                .build();
        repository = new TenantAwareClientRegistrationRepository(userManagementClient, fallback, TENANT_ID.toString());
    }

    @Test
    @DisplayName("SWR-063: returns tenant OIDC config when available")
    void returnsTenantConfig() {
        var settings = new TenantSettingsDto(
                TENANT_ID,
                "OIDC",
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                "https://auth.tenant.com",
                "tenant-client-id",
                "tenant-secret");
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settings);

        ClientRegistration result = repository.findByRegistrationId("authentik");

        assertThat(result.getClientId()).isEqualTo("tenant-client-id");
        assertThat(result.getClientSecret()).isEqualTo("tenant-secret");
        assertThat(result.getProviderDetails().getAuthorizationUri()).isEqualTo("https://auth.tenant.com/authorize/");
    }

    @Test
    @DisplayName("SWR-063: falls back when tenant has no OIDC config")
    void fallsBackWhenNoOidcConfig() {
        var settings = new TenantSettingsDto(
                TENANT_ID, "INTERNAL", false, Set.of(), "Blog", null, null, null, null, null, null);
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settings);

        ClientRegistration result = repository.findByRegistrationId("authentik");

        assertThat(result.getClientId()).isEqualTo("fallback-client");
    }

    @Test
    @DisplayName("SWR-063: falls back on service error")
    void fallsBackOnError() {
        when(userManagementClient.getTenantSettings(any())).thenThrow(new RuntimeException("down"));

        ClientRegistration result = repository.findByRegistrationId("authentik");

        assertThat(result.getClientId()).isEqualTo("fallback-client");
    }

    @Test
    @DisplayName("SWR-063: caches tenant config")
    void cachesTenantConfig() {
        var settings = new TenantSettingsDto(
                TENANT_ID,
                "OIDC",
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                "https://auth.tenant.com",
                "tenant-client-id",
                "tenant-secret");
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settings);

        repository.findByRegistrationId("authentik");
        repository.findByRegistrationId("authentik");

        verify(userManagementClient, times(1)).getTenantSettings(TENANT_ID);
    }

    @Test
    @DisplayName("SWR-063: handles issuer URL with trailing slash")
    void handlesTrailingSlash() {
        var settings = new TenantSettingsDto(
                TENANT_ID,
                "OIDC",
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                "https://auth.tenant.com/",
                "tenant-client-id",
                null);
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settings);

        ClientRegistration result = repository.findByRegistrationId("authentik");

        assertThat(result.getClientId()).isEqualTo("tenant-client-id");
        assertThat(result.getClientSecret()).isEmpty();
        assertThat(result.getProviderDetails().getAuthorizationUri()).isEqualTo("https://auth.tenant.com/authorize/");
    }

    @Test
    @DisplayName("SWR-063: falls back when only issuer URL set but no client ID")
    void fallsBackWhenOnlyIssuerUrl() {
        var settings = new TenantSettingsDto(
                TENANT_ID, "OIDC", false, Set.of(), "Blog", null, null, null, "https://auth.com", null, null);
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settings);

        ClientRegistration result = repository.findByRegistrationId("authentik");

        assertThat(result.getClientId()).isEqualTo("fallback-client");
    }
}
