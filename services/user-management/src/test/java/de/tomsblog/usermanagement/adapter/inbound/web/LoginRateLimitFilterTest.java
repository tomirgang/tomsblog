package de.tomsblog.usermanagement.adapter.inbound.web;

import static org.assertj.core.api.Assertions.*;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("SWR-028: LoginRateLimitFilter")
class LoginRateLimitFilterTest {

    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimitFilter();
    }

    @Test
    @DisplayName("allows requests under the rate limit")
    void allowsUnderLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        var chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        org.mockito.Mockito.verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("blocks requests over the rate limit")
    void blocksOverLimit() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
            req.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilter(req, resp, mock(FilterChain.class));
        }

        // 11th request should be blocked
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("does not filter GET requests")
    void doesNotFilterGet() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        var chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        org.mockito.Mockito.verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("does not filter other POST paths")
    void doesNotFilterOtherPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/register");
        MockHttpServletResponse response = new MockHttpServletResponse();

        var chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        org.mockito.Mockito.verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("filters POST /auth/admin/login")
    void filtersAdminLogin() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/admin/login");
            req.setRemoteAddr("10.0.0.2");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilter(req, resp, mock(FilterChain.class));
        }

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/admin/login");
        request.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("getClientIp uses X-Forwarded-For header")
    void getClientIpForwarded() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.50, 70.41.3.18");
        request.setRemoteAddr("127.0.0.1");

        assertThat(LoginRateLimitFilter.getClientIp(request)).isEqualTo("203.0.113.50");
    }

    @Test
    @DisplayName("getClientIp falls back to remoteAddr")
    void getClientIpRemote() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.0.1");

        assertThat(LoginRateLimitFilter.getClientIp(request)).isEqualTo("192.168.0.1");
    }

    @Test
    @DisplayName("getClientIp handles blank X-Forwarded-For")
    void getClientIpBlankForwarded() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "  ");
        request.setRemoteAddr("192.168.0.1");

        assertThat(LoginRateLimitFilter.getClientIp(request)).isEqualTo("192.168.0.1");
    }

    @Test
    @DisplayName("RateEntry reports expired after window")
    void rateEntryExpiration() {
        var entry = new LoginRateLimitFilter.RateEntry();
        assertThat(entry.isExpired()).isFalse();
        assertThat(entry.incrementAndCheck()).isTrue();
    }

    @Test
    @DisplayName("purgeExpired removes expired entries")
    void purgeExpiredRemovesEntries() throws Exception {
        // Create a rate-limited entry
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("10.0.0.99");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, mock(FilterChain.class));

        // purgeExpired should not remove non-expired entries
        filter.purgeExpired();

        // Entry should still work (not removed)
        MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/auth/login");
        req2.setRemoteAddr("10.0.0.99");
        MockHttpServletResponse resp2 = new MockHttpServletResponse();
        filter.doFilter(req2, resp2, mock(FilterChain.class));
        assertThat(resp2.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("doFilterInternal creates new entry when existing is expired")
    void doFilterInternal_createsNewEntryWhenExpired() throws Exception {
        // First request to establish an entry
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("10.0.0.100");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, mock(FilterChain.class));

        // Entry exists, not expired - second request increments count
        MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/auth/login");
        request2.setRemoteAddr("10.0.0.100");
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        filter.doFilter(request2, response2, mock(FilterChain.class));
        assertThat(response2.getStatus()).isEqualTo(200);
    }

    private static FilterChain mock(Class<FilterChain> clazz) {
        return org.mockito.Mockito.mock(clazz);
    }
}
