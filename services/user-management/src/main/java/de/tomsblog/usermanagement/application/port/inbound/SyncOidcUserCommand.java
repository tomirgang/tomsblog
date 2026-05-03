package de.tomsblog.usermanagement.application.port.inbound;

import java.util.List;

/**
 * Command to synchronize a user profile from OIDC claims.
 *
 * @req SWR-043
 */
public record SyncOidcUserCommand(String oidcSubject, String email, String displayName, List<String> oidcGroups) {

    public SyncOidcUserCommand {
        if (oidcSubject == null || oidcSubject.isBlank()) {
            throw new IllegalArgumentException("oidcSubject must not be blank");
        }
    }
}
