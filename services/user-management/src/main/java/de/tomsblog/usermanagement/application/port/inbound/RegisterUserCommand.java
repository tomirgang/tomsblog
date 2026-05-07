package de.tomsblog.usermanagement.application.port.inbound;

import de.tomsblog.shared.tenant.TenantId;
import java.util.Objects;

/**
 * Command to register a new local user with username and password.
 *
 * @req SWR-059
 */
public record RegisterUserCommand(
        String username, String password, String email, String displayName, TenantId tenantId) {

    public RegisterUserCommand {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password must not be blank");
        }
        if (password.length() < 12) {
            throw new IllegalArgumentException("password must be at least 12 characters long");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        Objects.requireNonNull(tenantId, "tenantId must not be null");
    }
}
