package de.tomsblog.usermanagement.application.port.inbound;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SyncInternalUserCommandTest {

    @Test
    @DisplayName("SWR-044: valid command creates successfully")
    void validCommandCreates() {
        var command = new SyncInternalUserCommand("admin", "hash", "admin@example.com", "Admin");

        assertThat(command.username()).isEqualTo("admin");
        assertThat(command.passwordHash()).isEqualTo("hash");
    }

    @Test
    @DisplayName("SWR-044: null username throws")
    void nullUsernameThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SyncInternalUserCommand(null, "hash", "email", "name"));
    }

    @Test
    @DisplayName("SWR-044: blank username throws")
    void blankUsernameThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SyncInternalUserCommand("  ", "hash", "email", "name"));
    }

    @Test
    @DisplayName("SWR-044: null passwordHash throws")
    void nullPasswordHashThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SyncInternalUserCommand("admin", null, "email", "name"));
    }

    @Test
    @DisplayName("SWR-044: blank passwordHash throws")
    void blankPasswordHashThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SyncInternalUserCommand("admin", "  ", "email", "name"));
    }
}
