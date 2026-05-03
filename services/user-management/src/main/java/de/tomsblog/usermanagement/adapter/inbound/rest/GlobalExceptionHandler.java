package de.tomsblog.usermanagement.adapter.inbound.rest;

import de.tomsblog.usermanagement.application.service.UserProfileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for REST controllers.
 *
 * @req SWR-043
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserProfileNotFoundException.class)
    public ProblemDetail handleUserProfileNotFound(UserProfileNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("User Profile Not Found");
        return problem;
    }
}
