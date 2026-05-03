package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.TenantInfoDto;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.TenantSettingsDto;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class TenantBrandingAdviceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private UserManagementClient userManagementClient;

    private TenantBrandingAdvice advice;

    @BeforeEach
    void setUp() {
        advice = new TenantBrandingAdvice(userManagementClient);
    }

    @Test
    @DisplayName("SWR-050: tenantName returns display name from settings")
    void tenantNameReturnsDisplayName() {
        var settings = new TenantSettingsDto(TENANT_ID, "BOTH", false, Set.of(), "My Custom Blog", null);
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settings);

        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", TENANT_ID.toString());
        var session = new MockHttpSession();

        String name = advice.tenantName(request, session);

        assertThat(name).isEqualTo("My Custom Blog");
    }

    @Test
    @DisplayName("SWR-050: tenantName returns default when display name is blank")
    void tenantNameDefaultsWhenBlank() {
        var settings = new TenantSettingsDto(TENANT_ID, "BOTH", false, Set.of(), "  ", null);
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settings);

        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", TENANT_ID.toString());
        var session = new MockHttpSession();

        String name = advice.tenantName(request, session);

        assertThat(name).isEqualTo("Toms Blog");
    }

    @Test
    @DisplayName("SWR-050: tenantName returns default when display name is null")
    void tenantNameDefaultsWhenNull() {
        var settings = new TenantSettingsDto(TENANT_ID, "BOTH", false, Set.of(), null, null);
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settings);

        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", TENANT_ID.toString());
        var session = new MockHttpSession();

        String name = advice.tenantName(request, session);

        assertThat(name).isEqualTo("Toms Blog");
    }

    @Test
    @DisplayName("SWR-050: tenantName returns default when client throws")
    void tenantNameDefaultsOnError() {
        when(userManagementClient.getTenantSettings(any())).thenThrow(new RuntimeException("down"));

        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", TENANT_ID.toString());
        var session = new MockHttpSession();

        String name = advice.tenantName(request, session);

        assertThat(name).isEqualTo("Toms Blog");
    }

    @Test
    @DisplayName("SWR-050: tenantName returns default when settings is null")
    void tenantNameDefaultsWhenSettingsNull() {
        when(userManagementClient.getTenantSettings(any())).thenReturn(null);

        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", TENANT_ID.toString());
        var session = new MockHttpSession();

        String name = advice.tenantName(request, session);

        assertThat(name).isEqualTo("Toms Blog");
    }

    @Test
    @DisplayName("SWR-050: tenantTagline returns tagline from settings")
    void tenantTaglineReturnsValue() {
        var settings = new TenantSettingsDto(TENANT_ID, "BOTH", false, Set.of(), "Blog", "A cool tagline");
        when(userManagementClient.getTenantSettings(TENANT_ID)).thenReturn(settings);

        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", TENANT_ID.toString());
        var session = new MockHttpSession();

        String tagline = advice.tenantTagline(request, session);

        assertThat(tagline).isEqualTo("A cool tagline");
    }

    @Test
    @DisplayName("SWR-050: tenantTagline returns null on error")
    void tenantTaglineNullOnError() {
        when(userManagementClient.getTenantSettings(any())).thenThrow(new RuntimeException("down"));

        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", TENANT_ID.toString());
        var session = new MockHttpSession();

        String tagline = advice.tenantTagline(request, session);

        assertThat(tagline).isNull();
    }

    @Test
    @DisplayName("SWR-050: tenantTagline returns null when settings is null")
    void tenantTaglineNullWhenSettingsNull() {
        when(userManagementClient.getTenantSettings(any())).thenReturn(null);

        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", TENANT_ID.toString());
        var session = new MockHttpSession();

        String tagline = advice.tenantTagline(request, session);

        assertThat(tagline).isNull();
    }

    @Test
    @DisplayName("SWR-053: tenants returns list for SUPERADMIN")
    void tenantsReturnsList() {
        var tenant = new TenantInfoDto(TENANT_ID, "Blog A");
        when(userManagementClient.listTenants()).thenReturn(List.of(tenant));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN")));

        List<TenantInfoDto> result = advice.tenants(auth);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("SWR-053: tenants returns empty for non-SUPERADMIN")
    void tenantsEmptyForNonSuperAdmin() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        List<TenantInfoDto> result = advice.tenants(auth);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("SWR-053: tenants returns empty when auth is null")
    void tenantsEmptyWhenAuthNull() {
        List<TenantInfoDto> result = advice.tenants(null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("SWR-053: tenants returns empty when client throws")
    void tenantsEmptyOnError() {
        when(userManagementClient.listTenants()).thenThrow(new RuntimeException("down"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN")));

        List<TenantInfoDto> result = advice.tenants(auth);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("SWR-053: activeTenantId returns header value")
    void activeTenantIdFromHeader() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", TENANT_ID.toString());
        var session = new MockHttpSession();

        String result = advice.activeTenantId(request, session);

        assertThat(result).isEqualTo(TENANT_ID.toString());
    }

    @Test
    @DisplayName("SWR-053: activeTenantId prefers session override")
    void activeTenantIdFromSession() {
        var sessionTenant = UUID.fromString("22222222-2222-2222-2222-222222222222");
        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", TENANT_ID.toString());
        var session = new MockHttpSession();
        session.setAttribute("activeTenantId", sessionTenant.toString());

        String result = advice.activeTenantId(request, session);

        assertThat(result).isEqualTo(sessionTenant.toString());
    }

    @Test
    @DisplayName("SWR-053: activeTenantId returns default when no header and no session")
    void activeTenantIdDefaultsWhenMissing() {
        var request = new MockHttpServletRequest();
        var session = new MockHttpSession();

        String result = advice.activeTenantId(request, session);

        assertThat(result).isEqualTo("00000000-0000-0000-0000-000000000001");
    }

    @Test
    @DisplayName("SWR-053: activeTenantId ignores invalid session value")
    void activeTenantIdIgnoresInvalidSession() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", TENANT_ID.toString());
        var session = new MockHttpSession();
        session.setAttribute("activeTenantId", "not-a-uuid");

        String result = advice.activeTenantId(request, session);

        assertThat(result).isEqualTo(TENANT_ID.toString());
    }

    @Test
    @DisplayName("SWR-053: resolveActiveTenantId ignores invalid header")
    void activeTenantIdIgnoresInvalidHeader() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "invalid");
        var session = new MockHttpSession();

        String result = advice.activeTenantId(request, session);

        assertThat(result).isEqualTo("00000000-0000-0000-0000-000000000001");
    }

    @Test
    @DisplayName("SWR-050: isSuperAdmin returns true for SUPERADMIN role")
    void isSuperAdminTrue() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN")));

        assertThat(advice.isSuperAdmin(auth)).isTrue();
    }

    @Test
    @DisplayName("SWR-050: isSuperAdmin returns false for non-SUPERADMIN")
    void isSuperAdminFalseForAdmin() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertThat(advice.isSuperAdmin(auth)).isFalse();
    }

    @Test
    @DisplayName("SWR-050: isSuperAdmin returns false when auth is null")
    void isSuperAdminFalseWhenNull() {
        assertThat(advice.isSuperAdmin(null)).isFalse();
    }
}
