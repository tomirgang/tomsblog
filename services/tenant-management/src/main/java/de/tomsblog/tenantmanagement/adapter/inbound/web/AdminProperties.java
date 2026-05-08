package de.tomsblog.tenantmanagement.adapter.inbound.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the break-glass SUPERADMIN credentials (ADR-0032).
 *
 * @req SWR-028
 * @req SWR-073
 */
@ConfigurationProperties(prefix = "blog.admin")
public record AdminProperties(String username, String password) {

    public AdminProperties {
        if (username == null || username.isBlank()) {
            username = "admin";
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "blog.admin.password must be configured. Set BLOG_ADMIN_PASSWORD environment variable.");
        }
    }
}
