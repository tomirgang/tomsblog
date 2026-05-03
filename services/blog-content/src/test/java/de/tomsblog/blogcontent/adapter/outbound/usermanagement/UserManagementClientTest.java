package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Tests for {@link UserManagementClient}.
 *
 * @req SWR-043
 */
class UserManagementClientTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private UserManagementClient client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() throws Exception {
        var properties = new UserManagementProperties("http://localhost:8081");
        client = new UserManagementClient(new RestTemplateBuilder(), properties);

        // Access the internal RestTemplate to bind MockRestServiceServer
        Field restTemplateField = UserManagementClient.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        RestTemplate restTemplate = (RestTemplate) restTemplateField.get(client);
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    @DisplayName("SWR-043: syncOidcUser sends POST and returns user profile")
    void syncOidcUser_sendsPostAndReturnsProfile() {
        UUID id = UUID.randomUUID();
        String responseJson = """
                {
                    "id": "%s",
                    "oidcSubject": "sub-123",
                    "username": null,
                    "authSource": "OIDC",
                    "email": "user@test.com",
                    "displayName": "Test User",
                    "approvalStatus": "APPROVED",
                    "globalRoles": ["ADMIN", "AUTHOR"],
                    "tenantMemberships": []
                }
                """.formatted(id);

        mockServer
                .expect(requestTo("http://localhost:8081/api/users/sync/oidc"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        UserProfileDto result =
                client.syncOidcUser("sub-123", "user@test.com", "Test User", List.of("group1"), TENANT_ID);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.oidcSubject()).isEqualTo("sub-123");
        assertThat(result.globalRoles()).containsExactly("ADMIN", "AUTHOR");
        mockServer.verify();
    }

    @Test
    @DisplayName("SWR-043: findByUsername sends GET and returns user profile")
    void findByUsername_sendsGetAndReturnsProfile() {
        UUID id = UUID.randomUUID();
        String responseJson = """
                {
                    "id": "%s",
                    "oidcSubject": null,
                    "username": "admin",
                    "authSource": "INTERNAL",
                    "email": "admin@test.com",
                    "displayName": "Admin",
                    "approvalStatus": "APPROVED",
                    "globalRoles": ["SUPERADMIN"],
                    "tenantMemberships": []
                }
                """.formatted(id);

        mockServer
                .expect(requestTo("http://localhost:8081/api/users/by-username/admin"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        UserProfileDto result = client.findByUsername("admin");

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("admin");
        assertThat(result.authSource()).isEqualTo("INTERNAL");
        mockServer.verify();
    }

    @Test
    @DisplayName("SWR-043: syncOidcUser throws on server error")
    void syncOidcUser_throwsOnServerError() {
        mockServer
                .expect(requestTo("http://localhost:8081/api/users/sync/oidc"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.syncOidcUser("sub-err", "err@test.com", "Error", List.of(), TENANT_ID))
                .isInstanceOf(RestClientException.class);
        mockServer.verify();
    }
}
