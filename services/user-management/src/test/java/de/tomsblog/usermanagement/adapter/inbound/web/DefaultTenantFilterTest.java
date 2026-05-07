package de.tomsblog.usermanagement.adapter.inbound.web;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("SWR-044: DefaultTenantFilter")
class DefaultTenantFilterTest {

    private DefaultTenantFilter filter;

    @BeforeEach
    void setUp() {
        filter = new DefaultTenantFilter();
        filter.setDefaultTenantId("00000000-0000-0000-0000-000000000001");
    }

    @Test
    @DisplayName("getDefaultTenantId returns configured value")
    void getDefaultTenantId() {
        assertThat(filter.getDefaultTenantId()).isEqualTo("00000000-0000-0000-0000-000000000001");
    }

    @Test
    @DisplayName("injects default tenant header when missing")
    void injectsDefaultTenantWhenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain)
                .doFilter(
                        argThat(req -> {
                            var httpReq = (jakarta.servlet.http.HttpServletRequest) req;
                            return "00000000-0000-0000-0000-000000000001".equals(httpReq.getHeader("X-Tenant-Id"));
                        }),
                        eq(response));
    }

    @Test
    @DisplayName("passes through valid X-Tenant-Id header")
    void passesThroughValidHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "11111111-1111-1111-1111-111111111111");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("rejects invalid X-Tenant-Id format with 400")
    void rejectsInvalidFormat() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "not-a-uuid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(400);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("uses session tenant override when present")
    void sessionTenantOverride() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "11111111-1111-1111-1111-111111111111");
        HttpSession session = request.getSession(true);
        session.setAttribute("activeTenantId", "22222222-2222-2222-2222-222222222222");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain)
                .doFilter(
                        argThat(req -> {
                            var httpReq = (jakarta.servlet.http.HttpServletRequest) req;
                            return "22222222-2222-2222-2222-222222222222".equals(httpReq.getHeader("X-Tenant-Id"));
                        }),
                        eq(response));
    }

    @Test
    @DisplayName("wrapper getHeaders returns overridden value")
    void wrapperGetHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, resp) -> {
            var httpReq = (jakarta.servlet.http.HttpServletRequest) req;
            var headers = httpReq.getHeaders("X-Tenant-Id");
            assertThat(headers.hasMoreElements()).isTrue();
            assertThat(headers.nextElement()).isEqualTo("00000000-0000-0000-0000-000000000001");
        };

        filter.doFilter(request, response, chain);
    }

    @Test
    @DisplayName("wrapper getHeaderNames includes injected header")
    void wrapperGetHeaderNames() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, resp) -> {
            var httpReq = (jakarta.servlet.http.HttpServletRequest) req;
            var names = httpReq.getHeaderNames();
            var nameList = java.util.Collections.list(names);
            assertThat(nameList).contains("X-Tenant-Id");
        };

        filter.doFilter(request, response, chain);
    }

    @Test
    @DisplayName("wrapper delegates getHeaders for non-tenant headers")
    void wrapperDelegatesOtherHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Custom", "custom-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, resp) -> {
            var httpReq = (jakarta.servlet.http.HttpServletRequest) req;
            var headers = httpReq.getHeaders("X-Custom");
            assertThat(headers.hasMoreElements()).isTrue();
            assertThat(headers.nextElement()).isEqualTo("custom-value");
        };

        filter.doFilter(request, response, chain);
    }
}
