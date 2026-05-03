package de.tomsblog.usermanagement.domain.model;

/**
 * Authentication source for a user profile.
 *
 * @req SWR-044
 */
public enum AuthSource {
    INTERNAL,
    OIDC
}
