package de.tomsblog.usermanagement.application.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserProfileNotFoundExceptionTest {

    @Test
    @DisplayName("SWR-043: exception contains identifier")
    void exceptionContainsIdentifier() {
        var ex = new UserProfileNotFoundException("sub-123");

        assertThat(ex.getMessage()).contains("sub-123");
        assertThat(ex.getIdentifier()).isEqualTo("sub-123");
    }
}
