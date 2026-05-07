package de.tomsblog.blogcontent.adapter.inbound.web;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.TenantSettingsDto;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * Tenant-aware client registration repository that dynamically resolves OIDC provider
 * configuration per tenant from the User Management Service.
 *
 * <p>Falls back to a global registration (from application.yml) when no tenant-specific
 * OIDC configuration exists.
 *
 * @req SWR-063
 * @req STK047
 */
public class TenantAwareClientRegistrationRepository implements ClientRegistrationRepository {

    private static final Logger LOG = LoggerFactory.getLogger(TenantAwareClientRegistrationRepository.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final UserManagementClient userManagementClient;
    private final ClientRegistration fallbackRegistration;
    private final String defaultTenantId;
    private final Map<String, CachedRegistration> cache = new ConcurrentHashMap<>();

    public TenantAwareClientRegistrationRepository(
            UserManagementClient userManagementClient,
            ClientRegistration fallbackRegistration,
            String defaultTenantId) {
        this.userManagementClient = userManagementClient;
        this.fallbackRegistration = fallbackRegistration;
        this.defaultTenantId = defaultTenantId;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        CachedRegistration cached = cache.get(defaultTenantId);
        if (cached != null && !cached.isExpired()) {
            return cached.registration();
        }

        try {
            TenantSettingsDto settings = userManagementClient.getTenantSettings(UUID.fromString(defaultTenantId));
            if (settings.oidcIssuerUrl() != null && settings.oidcClientId() != null) {
                ClientRegistration registration = buildRegistration(registrationId, settings);
                cache.put(defaultTenantId, new CachedRegistration(registration, Instant.now()));
                return registration;
            }
        } catch (Exception e) {
            LOG.warn("Failed to load tenant OIDC config for '{}', using fallback.", defaultTenantId, e);
        }

        return fallbackRegistration;
    }

    private ClientRegistration buildRegistration(String registrationId, TenantSettingsDto settings) {
        String issuerUrl =
                settings.oidcIssuerUrl().endsWith("/") ? settings.oidcIssuerUrl() : settings.oidcIssuerUrl() + "/";
        String wellKnown = issuerUrl + ".well-known/openid-configuration";

        return ClientRegistration.withRegistrationId(registrationId)
                .clientId(settings.oidcClientId())
                .clientSecret(settings.oidcClientSecret() != null ? settings.oidcClientSecret() : "")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri(issuerUrl + "authorize/")
                .tokenUri(issuerUrl + "token/")
                .userInfoUri(issuerUrl + "userinfo/")
                .jwkSetUri(issuerUrl + "jwks/")
                .providerConfigurationMetadata(Map.of("issuer", issuerUrl))
                .build();
    }

    private record CachedRegistration(ClientRegistration registration, Instant createdAt) {
        boolean isExpired() {
            return Instant.now().isAfter(createdAt.plus(CACHE_TTL));
        }
    }
}
