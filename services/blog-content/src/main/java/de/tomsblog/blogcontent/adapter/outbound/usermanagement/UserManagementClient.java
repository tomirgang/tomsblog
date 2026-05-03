package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import java.util.List;
import java.util.UUID;

/**
 * Client interface for communication with the User Management Service.
 * Implementations use gRPC for synchronous inter-service communication (SWR-046).
 *
 * @req SWR-043
 * @req SWR-046
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
     * Retrieves tenant settings (login mode, auto-approval) from the User Management Service.
     */
    TenantSettingsDto getTenantSettings(UUID tenantId);
}
