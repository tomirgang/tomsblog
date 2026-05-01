package de.tomsblog.blogcontent.adapter.inbound.rest;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OpenApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("SWR-024: OpenAPI spec is available at /api-docs")
    void openApiSpecAvailable() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"openapi\"");
        assertThat(response.getBody()).contains("Blog Content Service API");
    }

    @Test
    @DisplayName("SWR-024: Swagger UI is available")
    void swaggerUiAvailable() {
        ResponseEntity<String> response = restTemplate.getForEntity("/swagger-ui.html", String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.FOUND);
    }

    @Test
    @DisplayName("SWR-024: OpenAPI spec contains post endpoints")
    void openApiSpecContainsPostEndpoints() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api-docs", String.class);

        assertThat(response.getBody()).contains("/api/posts");
        assertThat(response.getBody()).contains("/api/tags");
        assertThat(response.getBody()).contains("/api/posts/{postId}/translations");
        assertThat(response.getBody()).contains("/api/posts/{postId}/sources");
    }

    @Test
    @DisplayName("SWR-024: OpenAPI spec contains X-Tenant-Id header parameter")
    void openApiSpecContainsTenantHeader() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api-docs", String.class);

        assertThat(response.getBody()).contains("X-Tenant-Id");
    }
}
