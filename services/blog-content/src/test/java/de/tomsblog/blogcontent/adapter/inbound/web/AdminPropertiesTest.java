package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminPropertiesTest {

    @Test
    @DisplayName("SWR-028: AdminProperties uses provided username and password")
    void providedValues_areUsed() {
        AdminProperties props = new AdminProperties("myuser", "mypass");
        assertThat(props.username()).isEqualTo("myuser");
        assertThat(props.password()).isEqualTo("mypass");
    }

    @Test
    @DisplayName("SWR-028: AdminProperties defaults username when null")
    void nullUsername_defaultsToAdmin() {
        AdminProperties props = new AdminProperties(null, "secret");
        assertThat(props.username()).isEqualTo("admin");
        assertThat(props.password()).isEqualTo("secret");
    }

    @Test
    @DisplayName("SWR-028: AdminProperties throws when password is null")
    void nullPassword_throwsException() {
        assertThatThrownBy(() -> new AdminProperties("user", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BLOG_ADMIN_PASSWORD");
    }

    @Test
    @DisplayName("SWR-028: AdminProperties defaults username when blank")
    void blankUsername_defaultsToAdmin() {
        AdminProperties props = new AdminProperties("  ", "secret");
        assertThat(props.username()).isEqualTo("admin");
    }

    @Test
    @DisplayName("SWR-028: AdminProperties throws when password is blank")
    void blankPassword_throwsException() {
        assertThatThrownBy(() -> new AdminProperties("user", "  "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BLOG_ADMIN_PASSWORD");
    }
}
