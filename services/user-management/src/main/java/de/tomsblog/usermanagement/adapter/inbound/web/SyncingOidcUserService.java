package de.tomsblog.usermanagement.adapter.inbound.web;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.SyncOidcUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.UserProfileUseCase;
import de.tomsblog.usermanagement.domain.model.Role;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Custom OIDC user service that synchronizes the user profile locally (ADR-0032).
 *
 * <p>Calls the local {@link UserProfileUseCase#syncFromOidc} directly instead of going through
 * gRPC. This is possible because the auth UI now lives in the User Management Service.
 *
 * @req SWR-016
 * @req SWR-043
 * @req SWR-045
 */
public class SyncingOidcUserService extends OidcUserService {

    private static final Logger LOG = LoggerFactory.getLogger(SyncingOidcUserService.class);

    private final UserProfileUseCase userProfileUseCase;
    private final UUID defaultTenantId;

    public SyncingOidcUserService(UserProfileUseCase userProfileUseCase, UUID defaultTenantId) {
        this.userProfileUseCase = userProfileUseCase;
        this.defaultTenantId = defaultTenantId;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegateLoadUser(userRequest);
        return enrichWithRoles(oidcUser);
    }

    OidcUser delegateLoadUser(OidcUserRequest userRequest) {
        return super.loadUser(userRequest);
    }

    OidcUser enrichWithRoles(OidcUser oidcUser) {
        String subject = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String displayName = oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getPreferredUsername();

        @SuppressWarnings("unchecked")
        List<String> groups =
                oidcUser.getClaimAsStringList("groups") != null ? oidcUser.getClaimAsStringList("groups") : List.of();

        try {
            UserProfile profile = userProfileUseCase.syncFromOidc(
                    new SyncOidcUserCommand(subject, email, displayName, groups, new TenantId(defaultTenantId)));
            Set<GrantedAuthority> authorities = mapAuthorities(profile, oidcUser.getAuthorities());
            return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        } catch (Exception e) {
            LOG.warn("Failed to sync user profile for OIDC subject '{}'. Using default authorities.", subject, e);
            return oidcUser;
        }
    }

    private Set<GrantedAuthority> mapAuthorities(
            UserProfile profile, Collection<? extends GrantedAuthority> defaultAuthorities) {
        Set<GrantedAuthority> authorities = new HashSet<>(defaultAuthorities);
        if (profile.getGlobalRoles() != null) {
            for (Role role : profile.getGlobalRoles()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
            }
        }
        return authorities;
    }
}
