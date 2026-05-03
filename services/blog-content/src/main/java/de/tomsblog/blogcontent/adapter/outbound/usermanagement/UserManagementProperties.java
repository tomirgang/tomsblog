package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the User Management Service connection.
 *
 * @req SWR-043
 */
@ConfigurationProperties(prefix = "blog.user-management")
public record UserManagementProperties(String baseUrl) {

    public UserManagementProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8081";
        }
    }
}
