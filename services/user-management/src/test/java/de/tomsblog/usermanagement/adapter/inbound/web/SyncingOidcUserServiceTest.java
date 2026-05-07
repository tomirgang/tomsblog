package de.tomsblog.usermanagement.adapter.inbound.web;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.tomsblog.usermanagement.application.port.inbound.UserProfileUseCase;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@DisplayName("SWR-016: SyncingOidcUserService")
class SyncingOidcUserServiceTest {

    private static final UUID DEFAULT_TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private UserProfileUseCase userProfileUseCase;
    private SyncingOidcUserService service;

    @BeforeEach
    void setUp() {
        userProfileUseCase = mock(UserProfileUseCase.class);
        service = new SyncingOidcUserService(userProfileUseCase, DEFAULT_TENANT);
    }

    private OidcUser createOidcUser(String subject, String email, String name, List<String> groups) {
        var claims = new java.util.HashMap<String, Object>();
        claims.put("sub", subject);
        claims.put("email", email);
        claims.put("name", name);
        if (groups != null) {
            claims.put("groups", groups);
        }
        claims.put("iss", "https://auth.example.com/");
        claims.put("aud", List.of("client-id"));
        claims.put("iat", java.time.Instant.now());

        var idToken = new OidcIdToken(
                "token-value", java.time.Instant.now(), java.time.Instant.now().plusSeconds(3600), claims);
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken);
    }

    @Test
    @DisplayName("enrichWithRoles syncs profile and maps global roles to authorities")
    void enrichWithRolesSuccess() {
        var profile = UserProfile.createFromOidc("sub-1", "user@test.com", "User");
        when(userProfileUseCase.syncFromOidc(any())).thenReturn(profile);

        OidcUser oidcUser = createOidcUser("sub-1", "user@test.com", "User", List.of());
        OidcUser result = service.enrichWithRoles(oidcUser);

        assertThat(result.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_READER"));
        assertThat(result.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }

    @Test
    @DisplayName("enrichWithRoles falls back to original user on exception")
    void enrichWithRolesFallback() {
        when(userProfileUseCase.syncFromOidc(any())).thenThrow(new RuntimeException("sync failed"));

        OidcUser oidcUser = createOidcUser("sub-1", "user@test.com", "User", null);
        OidcUser result = service.enrichWithRoles(oidcUser);

        assertThat(result).isSameAs(oidcUser);
    }

    @Test
    @DisplayName("enrichWithRoles uses preferredUsername when fullName is null")
    void usesPreferredUsername() {
        var profile = UserProfile.createFromOidc("sub-1", "user@test.com", "User");
        when(userProfileUseCase.syncFromOidc(any())).thenReturn(profile);

        // Create user without 'name' claim but with 'preferred_username'
        var claims = new java.util.HashMap<String, Object>();
        claims.put("sub", "sub-1");
        claims.put("email", "user@test.com");
        claims.put("preferred_username", "username1");
        claims.put("iss", "https://auth.example.com/");
        claims.put("aud", List.of("client-id"));
        claims.put("iat", java.time.Instant.now());
        var idToken = new OidcIdToken(
                "token", java.time.Instant.now(), java.time.Instant.now().plusSeconds(3600), claims);
        var oidcUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken);

        OidcUser result = service.enrichWithRoles(oidcUser);
        assertThat(result).isNotNull();
        verify(userProfileUseCase).syncFromOidc(any());
    }
}
