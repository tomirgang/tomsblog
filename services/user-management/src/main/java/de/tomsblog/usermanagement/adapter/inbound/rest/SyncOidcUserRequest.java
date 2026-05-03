package de.tomsblog.usermanagement.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Request to synchronize a user profile from OIDC claims.
 */
public record SyncOidcUserRequest(
        @NotBlank String oidcSubject, String email, String displayName, List<String> oidcGroups) {}
