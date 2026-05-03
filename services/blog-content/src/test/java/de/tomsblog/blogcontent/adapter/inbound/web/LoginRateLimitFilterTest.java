package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LoginRateLimitFilterTest {

    private LoginRateLimitFilter filter;
    private FilterChain filterChain;
    private int filterChainInvocations;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimitFilter();
        filterChainInvocations = 0;
        filterChain = (req, res) -> filterChainInvocations++;
    }

    @Test
    @DisplayName("GET request to /login is not filtered")
    void getLoginNotFiltered() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("POST to /login is filtered")
    void postLoginIsFiltered() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    @DisplayName("POST to /admin/login is filtered")
    void postAdminLoginIsFiltered() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/login");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    @DisplayName("POST to /api/posts is not filtered")
    void postOtherPathNotFiltered() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/posts");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("Requests within limit are allowed")
    void requestsWithinLimitAllowed() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(filterChainInvocations).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Requests exceeding limit return 429")
    void requestsExceedingLimitReturn429() throws ServletException, IOException {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
            request.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        // 11th request should be rate limited
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(filterChainInvocations).isEqualTo(10);
    }

    @Test
    @DisplayName("Different IPs are tracked independently")
    void differentIpsTrackedIndependently() throws ServletException, IOException {
        // Max out IP 1
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
            request.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, filterChain);
        }

        // IP 2 should still work
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChainInvocations).isEqualTo(11);
    }

    @Test
    @DisplayName("X-Forwarded-For header is used for client IP")
    void xForwardedForUsedForClientIp() throws ServletException, IOException {
        // Max out forwarded IP
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
            request.setRemoteAddr("127.0.0.1");
            request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, filterChain);
        }

        // Same forwarded IP should be blocked
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("Falls back to remoteAddr when X-Forwarded-For is blank")
    void fallsBackToRemoteAddrWhenForwardedIsBlank() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setRemoteAddr("192.168.1.50");
        request.addHeader("X-Forwarded-For", "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChainInvocations).isEqualTo(1);
    }

    @Test
    @DisplayName("Rate limited response contains error message")
    void rateLimitedResponseContainsMessage() throws ServletException, IOException {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
            request.setRemoteAddr("10.0.0.99");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, filterChain);
        }

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setRemoteAddr("10.0.0.99");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getContentAsString()).contains("Too many login attempts");
    }
}
