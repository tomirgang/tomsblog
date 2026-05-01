package de.tomsblog.blogcontent.adapter.inbound.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the admin user credentials.
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
            password = "admin";
        }
    }
}
