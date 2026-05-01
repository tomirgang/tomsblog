package de.tomsblog.blogcontent.adapter.inbound.web;

import de.tomsblog.blogcontent.application.service.PostNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception handler for Thymeleaf web controllers.
 *
 * @req SWR-026
 */
@ControllerAdvice(assignableTypes = BlogViewController.class)
public class WebExceptionHandler {

    @ExceptionHandler(PostNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handlePostNotFound() {
        return "error/404";
    }
}
