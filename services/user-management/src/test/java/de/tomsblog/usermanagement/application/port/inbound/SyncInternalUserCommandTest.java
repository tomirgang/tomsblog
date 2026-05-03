package de.tomsblog.usermanagement.application.port.inbound;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.shared.tenant.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SyncInternalUserCommandTest {

    private static final TenantId TENANT_ID = TenantId.generate();

    @Test
    @DisplayName("SWR-044: valid command creates successfully")
    void validCommandCreates() {
        var command = new SyncInternalUserCommand("admin", "hash", "admin@example.com", "Admin", TENANT_ID);

        assertThat(command.username()).isEqualTo("admin");
        assertThat(command.passwordHash()).isEqualTo("hash");
        assertThat(command.tenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    @DisplayName("SWR-044: null username throws")
    void nullUsernameThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SyncInternalUserCommand(null, "hash", "email", "name", TENANT_ID));
    }

    @Test
    @DisplayName("SWR-044: blank username throws")
    void blankUsernameThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SyncInternalUserCommand("  ", "hash", "email", "name", TENANT_ID));
    }

    @Test
    @DisplayName("SWR-044: null passwordHash throws")
    void nullPasswordHashThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SyncInternalUserCommand("admin", null, "email", "name", TENANT_ID));
    }

    @Test
    @DisplayName("SWR-044: blank passwordHash throws")
    void blankPasswordHashThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SyncInternalUserCommand("admin", "  ", "email", "name", TENANT_ID));
    }

    @Test
    @DisplayName("SWR-045: null tenantId throws")
    void nullTenantIdThrows() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SyncInternalUserCommand("admin", "hash", "email", "name", null));
    }
}
