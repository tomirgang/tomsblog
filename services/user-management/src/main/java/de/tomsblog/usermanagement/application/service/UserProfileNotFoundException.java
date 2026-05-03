package de.tomsblog.usermanagement.application.service;

/**
 * Exception thrown when a user profile is not found.
 *
 * @req SWR-043
 */
public class UserProfileNotFoundException extends RuntimeException {

    private final String identifier;

    public UserProfileNotFoundException(String identifier) {
        super("User profile not found: " + identifier);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
