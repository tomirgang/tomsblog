package de.tomsblog.usermanagement.domain;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.usermanagement.domain.model.UserProfileId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserProfileIdTest {

    @Test
    @DisplayName("SWR-043: generate creates unique ID")
    void generateCreatesUniqueId() {
        UserProfileId id1 = UserProfileId.generate();
        UserProfileId id2 = UserProfileId.generate();

        assertThat(id1).isNotEqualTo(id2);
        assertThat(id1.value()).isNotNull();
    }

    @Test
    @DisplayName("SWR-043: of creates ID from UUID")
    void ofCreatesIdFromUuid() {
        UUID uuid = UUID.randomUUID();

        UserProfileId id = UserProfileId.of(uuid);

        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    @DisplayName("SWR-043: null value rejected")
    void nullValueRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> new UserProfileId(null));
    }

    @Test
    @DisplayName("SWR-043: asString returns UUID string")
    void asStringReturnsUuidString() {
        UUID uuid = UUID.randomUUID();
        UserProfileId id = UserProfileId.of(uuid);

        assertThat(id.asString()).isEqualTo(uuid.toString());
    }
}
