package de.tomsblog.usermanagement.adapter.inbound.rest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("SWR-044: ApiKeyAuthenticationFilter")
class ApiKeyAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("authenticates request with valid API key")
    void validApiKey() throws Exception {
        var filter = new ApiKeyAuthenticationFilter("secret-key-123");
        var request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", "secret-key-123");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated())
                .isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo("service-client");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getCredentials())
                .isEqualTo("secret-key-123");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("does not authenticate with wrong API key")
    void wrongApiKey() throws Exception {
        var filter = new ApiKeyAuthenticationFilter("secret-key-123");
        var request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", "wrong-key");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("does not authenticate when API key is null")
    void nullApiKey() throws Exception {
        var filter = new ApiKeyAuthenticationFilter(null);
        var request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", "any-key");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("does not authenticate when no header present")
    void noHeader() throws Exception {
        var filter = new ApiKeyAuthenticationFilter("secret-key-123");
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}
