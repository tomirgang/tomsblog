package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link UserManagementProperties}.
 *
 * @req SWR-043
 */
class UserManagementPropertiesTest {

    @Test
    @DisplayName("SWR-043: uses provided base URL")
    void usesProvidedBaseUrl() {
        var props = new UserManagementProperties("http://user-management:8081");
        assertThat(props.baseUrl()).isEqualTo("http://user-management:8081");
    }

    @Test
    @DisplayName("SWR-043: defaults to localhost:8081 when null")
    void defaultsToLocalhostWhenNull() {
        var props = new UserManagementProperties(null);
        assertThat(props.baseUrl()).isEqualTo("http://localhost:8081");
    }

    @Test
    @DisplayName("SWR-043: defaults to localhost:8081 when blank")
    void defaultsToLocalhostWhenBlank() {
        var props = new UserManagementProperties("   ");
        assertThat(props.baseUrl()).isEqualTo("http://localhost:8081");
    }
}
