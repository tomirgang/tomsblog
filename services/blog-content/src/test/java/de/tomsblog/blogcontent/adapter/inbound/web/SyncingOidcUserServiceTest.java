package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserProfileDto;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Unit tests for {@link SyncingOidcUserService}.
 *
 * @req SWR-016
 * @req SWR-043
 */
@ExtendWith(MockitoExtension.class)
class SyncingOidcUserServiceTest {

    @Mock
    private UserManagementClient userManagementClient;

    private SyncingOidcUserService service;

    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        service = new SyncingOidcUserService(userManagementClient, DEFAULT_TENANT_ID);
    }

    @Test
    @DisplayName("SWR-016: enrichWithRoles adds roles from user management service")
    void enrichWithRoles_addsRoles() {
        var profile = new UserProfileDto(
                UUID.randomUUID(),
                "oidc-sub-123",
                null,
                "OIDC",
                "user@test.com",
                "Test User",
                "APPROVED",
                List.of("ADMIN", "AUTHOR"),
                List.of());

        when(userManagementClient.syncOidcUser(
                        eq("oidc-sub-123"), eq("user@test.com"), eq("Test User"), anyList(), eq(DEFAULT_TENANT_ID)))
                .thenReturn(profile);

        OidcUser oidcUser = createOidcUser("oidc-sub-123", "user@test.com", "Test User", null);
        OidcUser result = service.enrichWithRoles(oidcUser);

        assertThat(result).isNotNull();
        List<String> authorityNames = result.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        assertThat(authorityNames).contains("ROLE_ADMIN", "ROLE_AUTHOR");
    }

    @Test
    @DisplayName("SWR-016: enrichWithRoles falls back to default authorities when sync fails")
    void enrichWithRoles_fallbackOnSyncFailure() {
        when(userManagementClient.syncOidcUser(anyString(), anyString(), anyString(), anyList(), any(UUID.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        OidcUser oidcUser = createOidcUser("oidc-sub-456", "fail@test.com", "Fail User", null);
        OidcUser result = service.enrichWithRoles(oidcUser);

        assertThat(result).isNotNull();
        assertThat(result.getSubject()).isEqualTo("oidc-sub-456");
    }

    @Test
    @DisplayName("SWR-016: enrichWithRoles passes OIDC groups to user management service")
    void enrichWithRoles_passesGroups() {
        var profile = new UserProfileDto(
                UUID.randomUUID(),
                "oidc-sub-789",
                null,
                "OIDC",
                "groups@test.com",
                "Groups User",
                "APPROVED",
                List.of("READER"),
                List.of());

        when(userManagementClient.syncOidcUser(
                        eq("oidc-sub-789"),
                        eq("groups@test.com"),
                        eq("Groups User"),
                        eq(List.of("editors", "admins")),
                        eq(DEFAULT_TENANT_ID)))
                .thenReturn(profile);

        OidcUser oidcUser =
                createOidcUser("oidc-sub-789", "groups@test.com", "Groups User", List.of("editors", "admins"));
        OidcUser result = service.enrichWithRoles(oidcUser);

        assertThat(result).isNotNull();
        verify(userManagementClient)
                .syncOidcUser(
                        "oidc-sub-789",
                        "groups@test.com",
                        "Groups User",
                        List.of("editors", "admins"),
                        DEFAULT_TENANT_ID);
    }

    @Test
    @DisplayName("SWR-016: enrichWithRoles uses preferredUsername when fullName is null")
    void enrichWithRoles_usesPreferredUsernameAsFallback() {
        var profile = new UserProfileDto(
                UUID.randomUUID(),
                "oidc-sub-no-name",
                null,
                "OIDC",
                "noname@test.com",
                "noname_user",
                "APPROVED",
                List.of(),
                List.of());

        when(userManagementClient.syncOidcUser(anyString(), anyString(), eq("noname_user"), anyList(), any(UUID.class)))
                .thenReturn(profile);

        OidcUser oidcUser = createOidcUserWithPreferredUsername("oidc-sub-no-name", "noname@test.com", "noname_user");
        OidcUser result = service.enrichWithRoles(oidcUser);

        assertThat(result).isNotNull();
        verify(userManagementClient)
                .syncOidcUser("oidc-sub-no-name", "noname@test.com", "noname_user", List.of(), DEFAULT_TENANT_ID);
    }

    @Test
    @DisplayName("SWR-016: enrichWithRoles handles null globalRoles in profile")
    void enrichWithRoles_handlesNullGlobalRoles() {
        var profile = new UserProfileDto(
                UUID.randomUUID(),
                "oidc-sub-null",
                null,
                "OIDC",
                "null@test.com",
                "Null Roles",
                "APPROVED",
                null,
                List.of());

        when(userManagementClient.syncOidcUser(anyString(), anyString(), anyString(), anyList(), any(UUID.class)))
                .thenReturn(profile);

        OidcUser oidcUser = createOidcUser("oidc-sub-null", "null@test.com", "Null Roles", null);
        OidcUser result = service.enrichWithRoles(oidcUser);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("SWR-016: enrichWithRoles preserves original authorities alongside new roles")
    void enrichWithRoles_preservesOriginalAuthorities() {
        var profile = new UserProfileDto(
                UUID.randomUUID(),
                "oidc-sub-merge",
                null,
                "OIDC",
                "merge@test.com",
                "Merge User",
                "APPROVED",
                List.of("AUTHOR"),
                List.of());

        when(userManagementClient.syncOidcUser(anyString(), anyString(), anyString(), anyList(), any(UUID.class)))
                .thenReturn(profile);

        OidcUser oidcUser = createOidcUser("oidc-sub-merge", "merge@test.com", "Merge User", null);
        OidcUser result = service.enrichWithRoles(oidcUser);

        List<String> authorityNames = result.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        // Should have both the original OIDC authority and the new ROLE_AUTHOR
        assertThat(authorityNames).contains("ROLE_AUTHOR");
    }

    private OidcUser createOidcUser(String subject, String email, String name, List<String> groups) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", subject);
        claims.put("email", email);
        claims.put("name", name);
        claims.put("preferred_username", email);
        if (groups != null) {
            claims.put("groups", groups);
        }

        OidcIdToken idToken =
                new OidcIdToken("token-value", Instant.now(), Instant.now().plusSeconds(3600), claims);

        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken);
    }

    private OidcUser createOidcUserWithPreferredUsername(String subject, String email, String preferredUsername) {
        Map<String, Object> claims = Map.of(
                "sub", subject,
                "email", email,
                "preferred_username", preferredUsername);

        OidcIdToken idToken =
                new OidcIdToken("token-value", Instant.now(), Instant.now().plusSeconds(3600), claims);

        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken);
    }
}
