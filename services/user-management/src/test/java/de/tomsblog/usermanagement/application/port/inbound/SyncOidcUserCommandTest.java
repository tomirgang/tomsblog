package de.tomsblog.usermanagement.application.port.inbound;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SyncOidcUserCommandTest {

    private static final TenantId TENANT_ID = TenantId.generate();

    @Test
    @DisplayName("SWR-043: valid command creates successfully")
    void validCommandCreates() {
        var command = new SyncOidcUserCommand("sub-1", "user@example.com", "User", List.of("group1"), TENANT_ID);

        assertThat(command.oidcSubject()).isEqualTo("sub-1");
        assertThat(command.oidcGroups()).containsExactly("group1");
        assertThat(command.tenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    @DisplayName("SWR-043: null oidcSubject throws")
    void nullOidcSubjectThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SyncOidcUserCommand(null, "user@example.com", "User", List.of(), TENANT_ID));
    }

    @Test
    @DisplayName("SWR-043: blank oidcSubject throws")
    void blankOidcSubjectThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SyncOidcUserCommand("  ", "user@example.com", "User", List.of(), TENANT_ID));
    }

    @Test
    @DisplayName("SWR-045: null tenantId throws")
    void nullTenantIdThrows() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SyncOidcUserCommand("sub-1", "user@example.com", "User", List.of(), null));
    }
}
