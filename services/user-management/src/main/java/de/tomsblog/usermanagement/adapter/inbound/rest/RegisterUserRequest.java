package de.tomsblog.usermanagement.adapter.inbound.rest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request body for local user registration.
 *
 * @req SWR-059
 */
public record RegisterUserRequest(
        @NotBlank String username,

        @NotBlank
        @Size(min = 12)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit")
        String password,

        @NotBlank @Email String email,
        String displayName,
        @NotNull UUID tenantId) {}
