package de.tomsblog.usermanagement.application.service;

/**
 * Exception thrown when a user registration fails due to a duplicate username or email.
 *
 * @req SWR-059
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
