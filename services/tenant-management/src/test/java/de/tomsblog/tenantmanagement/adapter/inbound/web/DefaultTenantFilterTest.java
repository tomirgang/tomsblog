package de.tomsblog.tenantmanagement.adapter.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DefaultTenantFilterTest {

    @Test
    @DisplayName("SWR-072: injects default tenant when header missing")
    void injectsDefaultTenant() throws ServletException, IOException {
        var filter = new DefaultTenantFilter();
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        var filteredRequest = (jakarta.servlet.http.HttpServletRequest) chain.getRequest();
        assertThat(filteredRequest.getHeader("X-Tenant-Id")).isEqualTo("00000000-0000-0000-0000-000000000001");
    }

    @Test
    @DisplayName("SWR-072: passes through valid tenant header")
    void passesValidTenantHeader() throws ServletException, IOException {
        var filter = new DefaultTenantFilter();
        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "11111111-1111-1111-1111-111111111111");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        var filteredRequest = (jakarta.servlet.http.HttpServletRequest) chain.getRequest();
        assertThat(filteredRequest.getHeader("X-Tenant-Id")).isEqualTo("11111111-1111-1111-1111-111111111111");
    }

    @Test
    @DisplayName("SWR-072: rejects invalid UUID format")
    void rejectsInvalidUuid() throws ServletException, IOException {
        var filter = new DefaultTenantFilter();
        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "not-a-uuid");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("SWR-072: session tenant override takes precedence")
    void sessionTenantOverride() throws ServletException, IOException {
        var filter = new DefaultTenantFilter();
        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "11111111-1111-1111-1111-111111111111");
        var session = request.getSession(true);
        session.setAttribute("activeTenantId", "22222222-2222-2222-2222-222222222222");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        var filteredRequest = (jakarta.servlet.http.HttpServletRequest) chain.getRequest();
        assertThat(filteredRequest.getHeader("X-Tenant-Id")).isEqualTo("22222222-2222-2222-2222-222222222222");
    }

    @Test
    @DisplayName("SWR-072: defaultTenantId getter and setter work")
    void defaultTenantIdGetterSetter() {
        var filter = new DefaultTenantFilter();
        filter.setDefaultTenantId("33333333-3333-3333-3333-333333333333");
        assertThat(filter.getDefaultTenantId()).isEqualTo("33333333-3333-3333-3333-333333333333");
    }

    @Test
    @DisplayName("SWR-072: getHeaders returns overridden header")
    void getHeadersReturnsOverridden() throws ServletException, IOException {
        var filter = new DefaultTenantFilter();
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        var filteredRequest = (jakarta.servlet.http.HttpServletRequest) chain.getRequest();
        var headers = filteredRequest.getHeaders("X-Tenant-Id");
        assertThat(headers.hasMoreElements()).isTrue();
        assertThat(headers.nextElement()).isEqualTo("00000000-0000-0000-0000-000000000001");
    }

    @Test
    @DisplayName("SWR-072: getHeaderNames includes injected header")
    void getHeaderNamesIncludesInjected() throws ServletException, IOException {
        var filter = new DefaultTenantFilter();
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        var filteredRequest = (jakarta.servlet.http.HttpServletRequest) chain.getRequest();
        var names = filteredRequest.getHeaderNames();
        boolean found = false;
        while (names.hasMoreElements()) {
            if ("X-Tenant-Id".equals(names.nextElement())) {
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("SWR-072: getHeaders returns original when no override")
    void getHeadersReturnsOriginal() throws ServletException, IOException {
        var filter = new DefaultTenantFilter();
        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "11111111-1111-1111-1111-111111111111");
        request.addHeader("Accept", "text/html");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        var filteredRequest = (jakarta.servlet.http.HttpServletRequest) chain.getRequest();
        var acceptHeaders = filteredRequest.getHeaders("Accept");
        assertThat(acceptHeaders.hasMoreElements()).isTrue();
        assertThat(acceptHeaders.nextElement()).isEqualTo("text/html");
    }

    @Test
    @DisplayName("SWR-072: getHeader returns original when no override for that name")
    void getHeaderReturnsOriginalForNonOverridden() throws ServletException, IOException {
        var filter = new DefaultTenantFilter();
        var request = new MockHttpServletRequest();
        request.addHeader("Accept", "application/json");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        var filteredRequest = (jakarta.servlet.http.HttpServletRequest) chain.getRequest();
        assertThat(filteredRequest.getHeader("Accept")).isEqualTo("application/json");
    }

    @Test
    @DisplayName("SWR-072: session with no activeTenantId passes through")
    void sessionWithoutActiveTenantId() throws ServletException, IOException {
        var filter = new DefaultTenantFilter();
        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "11111111-1111-1111-1111-111111111111");
        request.getSession(true); // Create session but don't set activeTenantId
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        var filteredRequest = (jakarta.servlet.http.HttpServletRequest) chain.getRequest();
        assertThat(filteredRequest.getHeader("X-Tenant-Id")).isEqualTo("11111111-1111-1111-1111-111111111111");
    }
}
