package de.tomsblog.usermanagement.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to synchronize a user profile from internal login.
 */
public record SyncInternalUserRequest(
        @NotBlank String username, @NotBlank String passwordHash, String email, String displayName) {}
