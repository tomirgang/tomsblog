package de.tomsblog.tenantmanagement.adapter.inbound.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("SWR-072: handles IllegalArgumentException with 400")
    void handlesIllegalArgument() {
        var problem = handler.handleIllegalArgument(new IllegalArgumentException("bad"));
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("bad");
    }

    @Test
    @DisplayName("SWR-072: handles unexpected exception with 500")
    void handlesUnexpected() {
        var problem = handler.handleUnexpectedException(new RuntimeException("oops"));
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
