package de.tomsblog.usermanagement.adapter.outbound.persistence;

/**
 * JPA enum for user roles, separate from domain enum.
 */
public enum RoleJpa {
    SUPERADMIN,
    ADMIN,
    AUTHOR,
    REVIEWER,
    READER
}
