package de.tomsblog.blogcontent.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityAuditorAwareTest {

    private final SecurityAuditorAware auditorAware = new SecurityAuditorAware();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Returns username when authentication is present and authenticated")
    void returnsUsername_whenAuthenticated() {
        var auth = new TestingAuthenticationToken("testuser", "password");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).isPresent().contains("testuser");
    }

    @Test
    @DisplayName("Returns empty when no authentication is present")
    void returnsEmpty_whenNoAuthentication() {
        SecurityContextHolder.clearContext();

        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).isEmpty();
    }

    @Test
    @DisplayName("Returns empty when authentication is not authenticated")
    void returnsEmpty_whenNotAuthenticated() {
        var auth = new TestingAuthenticationToken("testuser", "password");
        auth.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).isEmpty();
    }
}
