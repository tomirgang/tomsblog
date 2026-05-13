package de.tomsblog.usermanagement.adapter.inbound.web;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.application.port.inbound.UserProfileUseCase;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.Role;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@DisplayName("SWR-016: SyncingOidcUserService")
class SyncingOidcUserServiceTest {

    private static final UUID DEFAULT_TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private UserProfileUseCase userProfileUseCase;
    private TenantSettingsUseCase tenantSettingsUseCase;
    private SyncingOidcUserService service;

    @BeforeEach
    void setUp() {
        userProfileUseCase = mock(UserProfileUseCase.class);
        tenantSettingsUseCase = mock(TenantSettingsUseCase.class);
        service = new SyncingOidcUserService(userProfileUseCase, tenantSettingsUseCase, DEFAULT_TENANT);
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

    @Test
    @DisplayName("loadUser delegates to super.loadUser and enriches with roles")
    void loadUserDelegatesToSuperAndEnriches() {
        var profile = UserProfile.createFromOidc("sub-1", "user@test.com", "User");
        when(userProfileUseCase.syncFromOidc(any())).thenReturn(profile);

        OidcUser mockOidcUser = createOidcUser("sub-1", "user@test.com", "User", List.of());
        SyncingOidcUserService spyService = spy(service);
        doReturn(mockOidcUser).when(spyService).delegateLoadUser(any(OidcUserRequest.class));

        OidcUser result = spyService.loadUser(mock(OidcUserRequest.class));

        assertThat(result).isNotNull();
        assertThat(result.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_READER"));
        verify(spyService).delegateLoadUser(any());
    }

    @Test
    @DisplayName("enrichWithRoles handles profile with null globalRoles")
    void enrichWithRolesNullGlobalRoles() {
        var profile = mock(UserProfile.class);
        when(profile.getGlobalRoles()).thenReturn(null);
        when(userProfileUseCase.syncFromOidc(any())).thenReturn(profile);

        OidcUser oidcUser = createOidcUser("sub-1", "user@test.com", "User", List.of());
        OidcUser result = service.enrichWithRoles(oidcUser);

        assertThat(result).isNotNull();
        assertThat(result.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }

    @Test
    @DisplayName("SWR-095: applyOidcRoleMapping assigns highest role from matching groups")
    void applyOidcRoleMappingAssignsHighestRole() {
        var profile = UserProfile.createFromOidc("sub-1", "user@test.com", "User");
        when(userProfileUseCase.syncFromOidc(any())).thenReturn(profile);

        var settings = TenantSettings.reconstitute(
                TenantId.of(DEFAULT_TENANT),
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
                true,
                Map.of("devs", "AUTHOR", "admins", "ADMIN"));
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        OidcUser oidcUser = createOidcUser("sub-1", "user@test.com", "User", List.of("devs", "admins"));
        service.enrichWithRoles(oidcUser);

        verify(userProfileUseCase).addTenantMembership("sub-1", TenantId.of(DEFAULT_TENANT), Role.ADMIN);
    }

    @Test
    @DisplayName("SWR-095: applyOidcRoleMapping does nothing when mapping disabled")
    void applyOidcRoleMappingDisabled() {
        var profile = UserProfile.createFromOidc("sub-1", "user@test.com", "User");
        when(userProfileUseCase.syncFromOidc(any())).thenReturn(profile);

        var settings = TenantSettings.reconstitute(
                TenantId.of(DEFAULT_TENANT),
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
                Map.of("devs", "AUTHOR"));
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        OidcUser oidcUser = createOidcUser("sub-1", "user@test.com", "User", List.of("devs"));
        service.enrichWithRoles(oidcUser);

        verify(userProfileUseCase, never()).addTenantMembership(any(), any(), any());
    }

    @Test
    @DisplayName("SWR-095: applyOidcRoleMapping does nothing when mappings are empty")
    void applyOidcRoleMappingEmptyMappings() {
        var profile = UserProfile.createFromOidc("sub-1", "user@test.com", "User");
        when(userProfileUseCase.syncFromOidc(any())).thenReturn(profile);

        var settings = TenantSettings.reconstitute(
                TenantId.of(DEFAULT_TENANT),
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
                true,
                Map.of());
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        OidcUser oidcUser = createOidcUser("sub-1", "user@test.com", "User", List.of("devs"));
        service.enrichWithRoles(oidcUser);

        verify(userProfileUseCase, never()).addTenantMembership(any(), any(), any());
    }

    @Test
    @DisplayName("SWR-095: applyOidcRoleMapping does nothing when no groups match")
    void applyOidcRoleMappingNoMatch() {
        var profile = UserProfile.createFromOidc("sub-1", "user@test.com", "User");
        when(userProfileUseCase.syncFromOidc(any())).thenReturn(profile);

        var settings = TenantSettings.reconstitute(
                TenantId.of(DEFAULT_TENANT),
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
                true,
                Map.of("other-group", "ADMIN"));
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        OidcUser oidcUser = createOidcUser("sub-1", "user@test.com", "User", List.of("devs"));
        service.enrichWithRoles(oidcUser);

        verify(userProfileUseCase, never()).addTenantMembership(any(), any(), any());
    }

    @Test
    @DisplayName("SWR-095: applyOidcRoleMapping skips SUPERADMIN mapping")
    void applyOidcRoleMappingSkipsSuperadmin() {
        var profile = UserProfile.createFromOidc("sub-1", "user@test.com", "User");
        when(userProfileUseCase.syncFromOidc(any())).thenReturn(profile);

        var settings = TenantSettings.reconstitute(
                TenantId.of(DEFAULT_TENANT),
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
                true,
                Map.of("super-group", "SUPERADMIN"));
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        OidcUser oidcUser = createOidcUser("sub-1", "user@test.com", "User", List.of("super-group"));
        service.enrichWithRoles(oidcUser);

        verify(userProfileUseCase, never()).addTenantMembership(any(), any(), any());
    }

    @Test
    @DisplayName("SWR-095: applyOidcRoleMapping skips SUPERADMIN but assigns next highest role")
    void applyOidcRoleMappingSkipsSuperadminButAssignsOther() {
        var profile = UserProfile.createFromOidc("sub-1", "user@test.com", "User");
        when(userProfileUseCase.syncFromOidc(any())).thenReturn(profile);

        var settings = TenantSettings.reconstitute(
                TenantId.of(DEFAULT_TENANT),
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
                true,
                Map.of("super-group", "SUPERADMIN", "devs", "AUTHOR"));
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        OidcUser oidcUser = createOidcUser("sub-1", "user@test.com", "User", List.of("super-group", "devs"));
        service.enrichWithRoles(oidcUser);

        verify(userProfileUseCase).addTenantMembership("sub-1", TenantId.of(DEFAULT_TENANT), Role.AUTHOR);
    }

    @Test
    @DisplayName("SWR-095: applyOidcRoleMapping handles empty groups list")
    void applyOidcRoleMappingEmptyGroups() {
        var profile = UserProfile.createFromOidc("sub-1", "user@test.com", "User");
        when(userProfileUseCase.syncFromOidc(any())).thenReturn(profile);

        var settings = TenantSettings.reconstitute(
                TenantId.of(DEFAULT_TENANT),
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
                true,
                Map.of("devs", "AUTHOR"));
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        OidcUser oidcUser = createOidcUser("sub-1", "user@test.com", "User", List.of());
        service.enrichWithRoles(oidcUser);

        verify(userProfileUseCase, never()).addTenantMembership(any(), any(), any());
    }

    @Test
    @DisplayName("SWR-095: applyOidcRoleMapping handles invalid role name gracefully")
    void applyOidcRoleMappingInvalidRole() {
        var profile = UserProfile.createFromOidc("sub-1", "user@test.com", "User");
        when(userProfileUseCase.syncFromOidc(any())).thenReturn(profile);

        var settings = TenantSettings.reconstitute(
                TenantId.of(DEFAULT_TENANT),
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
                true,
                Map.of("devs", "NOT_A_ROLE"));
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);

        OidcUser oidcUser = createOidcUser("sub-1", "user@test.com", "User", List.of("devs"));
        OidcUser result = service.enrichWithRoles(oidcUser);

        assertThat(result).isNotNull();
        verify(userProfileUseCase, never()).addTenantMembership(any(), any(), any());
    }

    @Test
    @DisplayName("SWR-095: applyOidcRoleMapping handles getSettings exception gracefully")
    void applyOidcRoleMappingHandlesException() {
        var profile = UserProfile.createFromOidc("sub-1", "user@test.com", "User");
        when(userProfileUseCase.syncFromOidc(any())).thenReturn(profile);
        when(tenantSettingsUseCase.getSettings(any())).thenThrow(new RuntimeException("settings error"));

        OidcUser oidcUser = createOidcUser("sub-1", "user@test.com", "User", List.of("devs"));
        OidcUser result = service.enrichWithRoles(oidcUser);

        assertThat(result).isNotNull();
        verify(userProfileUseCase, never()).addTenantMembership(any(), any(), any());
    }
}
