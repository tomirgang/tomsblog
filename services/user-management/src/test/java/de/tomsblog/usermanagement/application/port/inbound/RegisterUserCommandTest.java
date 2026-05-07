package de.tomsblog.usermanagement.application.port.inbound;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.shared.tenant.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegisterUserCommandTest {

    private static final TenantId TENANT_ID = TenantId.generate();

    @Test
    @DisplayName("SWR-059: valid command creates successfully")
    void validCommandCreates() {
        var command = new RegisterUserCommand("newuser", "securePassw0rd", "user@example.com", "User", TENANT_ID);

        assertThat(command.username()).isEqualTo("newuser");
        assertThat(command.password()).isEqualTo("securePassw0rd");
        assertThat(command.email()).isEqualTo("user@example.com");
        assertThat(command.tenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    @DisplayName("SWR-059: null username throws")
    void nullUsernameThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> new RegisterUserCommand(null, "securePassw0rd", "user@example.com", "User", TENANT_ID));
    }

    @Test
    @DisplayName("SWR-059: blank username throws")
    void blankUsernameThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> new RegisterUserCommand("  ", "securePassw0rd", "user@example.com", "User", TENANT_ID));
    }

    @Test
    @DisplayName("SWR-059: null password throws")
    void nullPasswordThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RegisterUserCommand("newuser", null, "user@example.com", "User", TENANT_ID));
    }

    @Test
    @DisplayName("SWR-059: blank password throws")
    void blankPasswordThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RegisterUserCommand("newuser", "  ", "user@example.com", "User", TENANT_ID));
    }

    @Test
    @DisplayName("SWR-059: short password throws")
    void shortPasswordThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RegisterUserCommand("newuser", "short", "user@example.com", "User", TENANT_ID));
    }

    @Test
    @DisplayName("SWR-059: password exactly 12 chars is valid")
    void exactlyTwelveCharsIsValid() {
        var command = new RegisterUserCommand("newuser", "123456789012", "user@example.com", "User", TENANT_ID);
        assertThat(command.password()).hasSize(12);
    }

    @Test
    @DisplayName("SWR-059: null email throws")
    void nullEmailThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RegisterUserCommand("newuser", "securePassw0rd", null, "User", TENANT_ID));
    }

    @Test
    @DisplayName("SWR-059: blank email throws")
    void blankEmailThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RegisterUserCommand("newuser", "securePassw0rd", "  ", "User", TENANT_ID));
    }

    @Test
    @DisplayName("SWR-059: null tenantId throws")
    void nullTenantIdThrows() {
        assertThatNullPointerException()
                .isThrownBy(
                        () -> new RegisterUserCommand("newuser", "securePassw0rd", "user@example.com", "User", null));
    }
}
