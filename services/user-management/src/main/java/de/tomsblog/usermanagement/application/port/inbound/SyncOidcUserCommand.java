package de.tomsblog.usermanagement.application.port.inbound;

import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.Objects;

/**
 * Command to synchronize a user profile from OIDC claims.
 *
 * @req SWR-043
 * @req SWR-045
 */
public record SyncOidcUserCommand(
        String oidcSubject, String email, String displayName, List<String> oidcGroups, TenantId tenantId) {

    public SyncOidcUserCommand {
        if (oidcSubject == null || oidcSubject.isBlank()) {
            throw new IllegalArgumentException("oidcSubject must not be blank");
        }
        Objects.requireNonNull(tenantId, "tenantId must not be null");
    }
}
