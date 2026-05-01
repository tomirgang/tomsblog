package de.tomsblog.blogcontent.adapter.inbound.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that injects default X-Tenant-Id and X-Author-Id headers when missing. The default values
 * are configurable via properties (blog.default-tenant-id, blog.default-author-id).
 *
 * <p>This enables the Thymeleaf UI to work in a browser without requiring manual header injection.
 * In a later phase, this filter will be replaced by domain-based tenant resolution and proper
 * authentication (ADR-0012).
 */
@Component
@ConfigurationProperties(prefix = "blog")
public class DefaultTenantFilter extends OncePerRequestFilter {

    private String defaultTenantId = "00000000-0000-0000-0000-000000000001";
    private String defaultAuthorId = "00000000-0000-0000-0000-000000000001";

    public String getDefaultTenantId() {
        return defaultTenantId;
    }

    public void setDefaultTenantId(String defaultTenantId) {
        this.defaultTenantId = defaultTenantId;
    }

    public String getDefaultAuthorId() {
        return defaultAuthorId;
    }

    public void setDefaultAuthorId(String defaultAuthorId) {
        this.defaultAuthorId = defaultAuthorId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean missingTenant = request.getHeader("X-Tenant-Id") == null;
        boolean missingAuthor = request.getHeader("X-Author-Id") == null;

        if (missingTenant || missingAuthor) {
            filterChain.doFilter(new DefaultHeaderRequestWrapper(request, missingTenant, missingAuthor), response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private class DefaultHeaderRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> additionalHeaders = new HashMap<>();

        DefaultHeaderRequestWrapper(HttpServletRequest request, boolean addTenant, boolean addAuthor) {
            super(request);
            if (addTenant) {
                additionalHeaders.put("X-Tenant-Id", defaultTenantId);
            }
            if (addAuthor) {
                additionalHeaders.put("X-Author-Id", defaultAuthorId);
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
