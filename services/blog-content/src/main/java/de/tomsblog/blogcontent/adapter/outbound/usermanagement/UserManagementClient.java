package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import java.util.List;
import java.util.UUID;

/**
 * Client interface for communication with the User Management Service.
 * Implementations use gRPC for synchronous inter-service communication (SWR-046).
 *
 * @req SWR-043
 * @req SWR-046
 * @req SWR-050
 * @req SWR-051
 * @req SWR-052
 * @req SWR-053
 */
public interface UserManagementClient {

    /**
     * Synchronizes an OIDC user profile. Creates or updates the profile and returns it with roles.
     */
    UserProfileDto syncOidcUser(
            String oidcSubject, String email, String displayName, List<String> groups, UUID tenantId);

    /**
     * Retrieves a user profile by username.
     */
    UserProfileDto findByUsername(String username);

    /**
     * Retrieves tenant settings (login mode, auto-approval, branding) from the User Management Service.
     */
    TenantSettingsDto getTenantSettings(UUID tenantId);

    /**
     * Lists all users belonging to a specific tenant.
     */
    List<UserProfileDto> listUsersByTenant(UUID tenantId);

    /**
     * Approves a user.
     */
    void approveUser(String identifier);

    /**
     * Rejects a user.
     */
    void rejectUser(String identifier);

    /**
     * Changes a user's role in a tenant.
     */
    void changeUserRole(String identifier, UUID tenantId, String role);

    /**
     * Updates tenant settings.
     */
    TenantSettingsDto updateTenantSettings(
            UUID tenantId,
            String loginMode,
            boolean autoApproveOidc,
            java.util.Set<String> autoApproveEmailDomains,
            String displayName,
            String tagline,
            String impressumContent,
            String privacyPolicyContent,
            String oidcIssuerUrl,
            String oidcClientId,
            String oidcClientSecret,
            String oidcButtonText,
            String defaultRole,
            String logoUrl,
            String faviconUrl,
            boolean oidcRoleMappingEnabled,
            java.util.Map<String, String> oidcRoleMappings);

    /**
     * Lists all tenants.
     */
    List<TenantInfoDto> listTenants();

    /**
     * Registers a new local user (SWR-059).
     */
    UserProfileDto registerUser(String username, String password, String email, String displayName, UUID tenantId);
}
