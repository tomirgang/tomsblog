package de.tomsblog.usermanagement.adapter.inbound.web;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SWR-028: AdminProperties")
class AdminPropertiesTest {

    @Test
    @DisplayName("uses 'admin' as default username when null")
    void defaultUsernameWhenNull() {
        var props = new AdminProperties(null, "secret-password");
        assertThat(props.username()).isEqualTo("admin");
    }

    @Test
    @DisplayName("uses 'admin' as default username when blank")
    void defaultUsernameWhenBlank() {
        var props = new AdminProperties("  ", "secret-password");
        assertThat(props.username()).isEqualTo("admin");
    }

    @Test
    @DisplayName("uses provided username when present")
    void customUsername() {
        var props = new AdminProperties("superadmin", "secret-password");
        assertThat(props.username()).isEqualTo("superadmin");
    }

    @Test
    @DisplayName("throws when password is null")
    void throwsWhenPasswordNull() {
        assertThatThrownBy(() -> new AdminProperties("admin", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("blog.admin.password must be configured");
    }

    @Test
    @DisplayName("throws when password is blank")
    void throwsWhenPasswordBlank() {
        assertThatThrownBy(() -> new AdminProperties("admin", "  "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("blog.admin.password must be configured");
    }
}
