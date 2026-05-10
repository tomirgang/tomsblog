package de.tomsblog.usermanagement.adapter.inbound.web;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
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
 * Tenant-aware client registration repository that resolves OIDC provider configuration
 * per tenant from the local database (ADR-0032).
 *
 * <p>Falls back to a global registration (from application.yml) when no tenant-specific
 * OIDC configuration exists.
 *
 * @req SWR-063
 */
public class TenantAwareClientRegistrationRepository implements ClientRegistrationRepository {

    private static final Logger LOG = LoggerFactory.getLogger(TenantAwareClientRegistrationRepository.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final TenantSettingsUseCase tenantSettingsUseCase;
    private final ClientRegistration fallbackRegistration;
    private final String defaultTenantId;
    private final Map<String, CachedRegistration> cache = new ConcurrentHashMap<>();

    public TenantAwareClientRegistrationRepository(
            TenantSettingsUseCase tenantSettingsUseCase,
            ClientRegistration fallbackRegistration,
            String defaultTenantId) {
        this.tenantSettingsUseCase = tenantSettingsUseCase;
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
            TenantSettings settings = tenantSettingsUseCase.getSettings(new TenantId(UUID.fromString(defaultTenantId)));
            if (settings != null && settings.getOidcIssuerUrl() != null && settings.getOidcClientId() != null) {
                ClientRegistration registration = buildRegistration(registrationId, settings);
                cache.put(defaultTenantId, new CachedRegistration(registration, Instant.now()));
                return registration;
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("Failed to load tenant OIDC config for '{}', using fallback.", defaultTenantId, e);
        }

        return fallbackRegistration;
    }

    private ClientRegistration buildRegistration(String registrationId, TenantSettings settings) {
        String issuerUrl = settings.getOidcIssuerUrl().endsWith("/")
                ? settings.getOidcIssuerUrl()
                : settings.getOidcIssuerUrl() + "/";

        validateIssuerUrl(issuerUrl);

        return ClientRegistration.withRegistrationId(registrationId)
                .clientId(settings.getOidcClientId())
                .clientSecret(settings.getOidcClientSecret() != null ? settings.getOidcClientSecret() : "")
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

    private static void validateIssuerUrl(String issuerUrl) {
        var uri = java.net.URI.create(issuerUrl);
        if (!"https".equals(uri.getScheme()) && !"http".equals(uri.getScheme())) {
            throw new IllegalArgumentException("OIDC issuer URL must use HTTP(S) scheme");
        }
        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("OIDC issuer URL must have a host");
        }
        if (host.endsWith(".svc.cluster.local")
                || "localhost".equals(host)
                || host.startsWith("10.")
                || host.startsWith("192.168.")
                || host.startsWith("172.16.")
                || host.startsWith("127.")) {
            throw new IllegalArgumentException("OIDC issuer URL must not point to internal services");
        }
    }

    private record CachedRegistration(ClientRegistration registration, Instant createdAt) {
        boolean isExpired() {
            return Instant.now().isAfter(createdAt.plus(CACHE_TTL));
        }
    }
}
