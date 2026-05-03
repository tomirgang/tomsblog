package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for deprecated {@link UserManagementProperties}.
 * This class verifies the deprecated marker exists (to be removed when class is deleted).
 */
@SuppressWarnings("removal")
class UserManagementPropertiesTest {

    @Test
    @DisplayName("SWR-046: UserManagementProperties is deprecated")
    void isDeprecated() {
        assertThat(UserManagementProperties.class.isAnnotationPresent(Deprecated.class))
                .isTrue();
    }
}
