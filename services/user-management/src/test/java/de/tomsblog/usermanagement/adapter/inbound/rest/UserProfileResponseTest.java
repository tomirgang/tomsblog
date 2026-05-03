package de.tomsblog.usermanagement.adapter.inbound.rest;

import static org.assertj.core.api.Assertions.*;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.domain.model.Role;
import de.tomsblog.usermanagement.domain.model.TenantMembership;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserProfileResponseTest {

    @Test
    @DisplayName("SWR-043: from maps user with tenant memberships")
    void fromMapsUserWithTenantMemberships() {
        TenantId tenantId = TenantId.generate();
        UserProfile profile = UserProfile.createFromOidc("sub-1", "user@example.com", "User");
        profile.addTenantMembership(new TenantMembership(tenantId, Role.AUTHOR));

        UserProfileResponse response = UserProfileResponse.from(profile);

        assertThat(response.tenantMemberships()).hasSize(1);
        assertThat(response.tenantMemberships().getFirst().tenantId())
                .isEqualTo(tenantId.value().toString());
        assertThat(response.tenantMemberships().getFirst().role()).isEqualTo(Role.AUTHOR);
    }

    @Test
    @DisplayName("SWR-043: from maps user without tenant memberships")
    void fromMapsUserWithoutTenantMemberships() {
        UserProfile profile = UserProfile.createFromOidc("sub-1", "user@example.com", "User");

        UserProfileResponse response = UserProfileResponse.from(profile);

        assertThat(response.tenantMemberships()).isEmpty();
        assertThat(response.oidcSubject()).isEqualTo("sub-1");
        assertThat(response.email()).isEqualTo("user@example.com");
    }
}
