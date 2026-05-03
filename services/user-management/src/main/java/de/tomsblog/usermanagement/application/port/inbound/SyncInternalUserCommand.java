package de.tomsblog.usermanagement.application.port.inbound;

import de.tomsblog.shared.tenant.TenantId;
import java.util.Objects;

/**
 * Command to synchronize a user profile from internal (form-based) login.
 *
 * @req SWR-043
 * @req SWR-044
 * @req SWR-045
 */
public record SyncInternalUserCommand(
        String username, String passwordHash, String email, String displayName, TenantId tenantId) {

    public SyncInternalUserCommand {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
        Objects.requireNonNull(tenantId, "tenantId must not be null");
    }
}
