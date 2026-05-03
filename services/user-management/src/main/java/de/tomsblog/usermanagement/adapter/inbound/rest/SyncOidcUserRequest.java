package de.tomsblog.usermanagement.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Request to synchronize a user profile from OIDC claims.
 */
public record SyncOidcUserRequest(
        @NotBlank String oidcSubject,
        String email,
        String displayName,
        List<String> oidcGroups,
        @NotNull UUID tenantId) {}
