package de.tomsblog.usermanagement.application.port.inbound;

/**
 * Command to synchronize a user profile from internal (form-based) login.
 *
 * @req SWR-043
 * @req SWR-044
 */
public record SyncInternalUserCommand(String username, String passwordHash, String email, String displayName) {

    public SyncInternalUserCommand {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
    }
}
