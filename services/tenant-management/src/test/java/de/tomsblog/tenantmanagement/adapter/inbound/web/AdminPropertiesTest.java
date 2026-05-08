package de.tomsblog.tenantmanagement.adapter.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminPropertiesTest {

    @Test
    @DisplayName("SWR-073: default username is admin")
    void defaultUsername() {
        var props = new AdminProperties(null, "secret");
        assertThat(props.username()).isEqualTo("admin");
    }

    @Test
    @DisplayName("SWR-073: blank username defaults to admin")
    void blankUsernameDefaults() {
        var props = new AdminProperties("  ", "secret");
        assertThat(props.username()).isEqualTo("admin");
    }

    @Test
    @DisplayName("SWR-073: custom username is preserved")
    void customUsername() {
        var props = new AdminProperties("superadmin", "secret");
        assertThat(props.username()).isEqualTo("superadmin");
    }

    @Test
    @DisplayName("SWR-073: null password throws")
    void nullPasswordThrows() {
        assertThatThrownBy(() -> new AdminProperties("admin", null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("SWR-073: blank password throws")
    void blankPasswordThrows() {
        assertThatThrownBy(() -> new AdminProperties("admin", "  ")).isInstanceOf(IllegalStateException.class);
    }
}
