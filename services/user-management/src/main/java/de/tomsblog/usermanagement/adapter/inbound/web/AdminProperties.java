package de.tomsblog.usermanagement.adapter.inbound.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the break-glass SUPERADMIN credentials (ADR-0032).
 *
 * <p>The password must be configured via the BLOG_ADMIN_PASSWORD environment variable. The
 * application will fail to start if no password is provided.
 *
 * @req SWR-028
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
