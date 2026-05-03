package de.tomsblog.blogcontent.adapter.inbound.web;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserProfileDto;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
 * Custom OIDC user service that synchronizes the user profile with the User Management Service
 * and enriches the Spring Security principal with platform roles.
 *
 * @req SWR-016
 * @req SWR-043
 */
public class SyncingOidcUserService extends OidcUserService {

    private static final Logger LOG = LoggerFactory.getLogger(SyncingOidcUserService.class);

    private final UserManagementClient userManagementClient;

    public SyncingOidcUserService(UserManagementClient userManagementClient) {
        this.userManagementClient = userManagementClient;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        return enrichWithRoles(oidcUser);
    }

    OidcUser enrichWithRoles(OidcUser oidcUser) {
        String subject = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String displayName = oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getPreferredUsername();

        @SuppressWarnings("unchecked")
        List<String> groups =
                oidcUser.getClaimAsStringList("groups") != null ? oidcUser.getClaimAsStringList("groups") : List.of();

        try {
            UserProfileDto profile = userManagementClient.syncOidcUser(subject, email, displayName, groups);
            Set<GrantedAuthority> authorities = mapAuthorities(profile, oidcUser.getAuthorities());
            return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        } catch (Exception e) {
            LOG.warn("Failed to sync user profile for OIDC subject '{}'. Using default authorities.", subject, e);
            return oidcUser;
        }
    }

    private Set<GrantedAuthority> mapAuthorities(
            UserProfileDto profile, Collection<? extends GrantedAuthority> defaultAuthorities) {
        Set<GrantedAuthority> authorities = new HashSet<>(defaultAuthorities);
        if (profile.globalRoles() != null) {
            for (String role : profile.globalRoles()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
        }
        return authorities;
    }
}
