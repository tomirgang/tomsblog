package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import java.util.List;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * REST client for communication with the User Management Service.
 *
 * @req SWR-043
 * @req SWR-016
 */
@Component
public class UserManagementClient {

    private final RestTemplate restTemplate;

    public UserManagementClient(RestTemplateBuilder restTemplateBuilder, UserManagementProperties properties) {
        this.restTemplate = restTemplateBuilder.rootUri(properties.baseUrl()).build();
    }

    /**
     * Synchronizes an OIDC user profile. Creates or updates the profile and returns it with roles.
     */
    public UserProfileDto syncOidcUser(String oidcSubject, String email, String displayName, List<String> groups) {
        var request = new SyncOidcUserDto(oidcSubject, email, displayName, groups);
        return restTemplate.postForObject("/api/users/sync/oidc", request, UserProfileDto.class);
    }

    /**
     * Retrieves a user profile by username.
     */
    public UserProfileDto findByUsername(String username) {
        return restTemplate.getForObject("/api/users/by-username/{username}", UserProfileDto.class, username);
    }
}
