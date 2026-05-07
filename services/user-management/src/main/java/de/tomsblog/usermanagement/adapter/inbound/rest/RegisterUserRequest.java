package de.tomsblog.usermanagement.adapter.inbound.rest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request body for local user registration.
 *
 * @req SWR-059
 */
public record RegisterUserRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 12) String password,
        @NotBlank @Email String email,
        String displayName,
        @NotNull UUID tenantId) {}
