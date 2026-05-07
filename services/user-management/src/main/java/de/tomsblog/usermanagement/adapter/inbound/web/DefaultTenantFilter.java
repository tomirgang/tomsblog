package de.tomsblog.usermanagement.adapter.inbound.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that injects a default X-Tenant-Id header when missing (ADR-0032).
 *
 * <p>Validates that provided X-Tenant-Id headers are well-formed UUIDs. In production, the API
 * gateway will set this header based on the Host header (ADR-0012).
 */
@Component
@ConfigurationProperties(prefix = "blog")
public class DefaultTenantFilter extends OncePerRequestFilter {

    private String defaultTenantId = "00000000-0000-0000-0000-000000000001";

    public String getDefaultTenantId() {
        return defaultTenantId;
    }

    public void setDefaultTenantId(String defaultTenantId) {
        this.defaultTenantId = defaultTenantId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String sessionTenantOverride = resolveSessionTenant(request);
        String providedTenantId = request.getHeader("X-Tenant-Id");
        boolean missingTenant = providedTenantId == null;

        if (!missingTenant && !isValidUuid(providedTenantId)) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid X-Tenant-Id format");
            return;
        }

        if (sessionTenantOverride != null || missingTenant) {
            filterChain.doFilter(
                    new DefaultHeaderRequestWrapper(request, missingTenant, sessionTenantOverride), response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private String resolveSessionTenant(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            Object value = session.getAttribute("activeTenantId");
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static boolean isValidUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private class DefaultHeaderRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> additionalHeaders = new HashMap<>();

        DefaultHeaderRequestWrapper(HttpServletRequest request, boolean addTenant, String sessionTenantOverride) {
            super(request);
            if (sessionTenantOverride != null) {
                additionalHeaders.put("X-Tenant-Id", sessionTenantOverride);
            } else if (addTenant) {
                additionalHeaders.put("X-Tenant-Id", defaultTenantId);
            }
        }

        @Override
        public String getHeader(String name) {
            String value = additionalHeaders.get(name);
            return value != null ? value : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String value = additionalHeaders.get(name);
            if (value != null) {
                return Collections.enumeration(List.of(value));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>(additionalHeaders.keySet());
            Enumeration<String> originalNames = super.getHeaderNames();
            while (originalNames.hasMoreElements()) {
                names.add(originalNames.nextElement());
            }
            return Collections.enumeration(names);
        }
    }
}
