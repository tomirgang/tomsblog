package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TenantSettingsDtoTest {

    private static final UUID TENANT_ID = UUID.randomUUID();

    @Test
    @DisplayName("SWR-061: hasOidcClientSecret returns true when secret is set")
    void hasOidcClientSecretTrue() {
        var dto = new TenantSettingsDto(
                TENANT_ID, "OIDC", false, Set.of(), "Blog", null, null, null, null, null, "secret", null, null, null,
                null, false, Map.of());
        assertThat(dto.hasOidcClientSecret()).isTrue();
    }

    @Test
    @DisplayName("SWR-061: hasOidcClientSecret returns false when secret is null")
    void hasOidcClientSecretFalseNull() {
        var dto = new TenantSettingsDto(
                TENANT_ID, "OIDC", false, Set.of(), "Blog", null, null, null, null, null, null, null, null, null, null,
                false, Map.of());
        assertThat(dto.hasOidcClientSecret()).isFalse();
    }

    @Test
    @DisplayName("SWR-061: hasOidcClientSecret returns false when secret is empty")
    void hasOidcClientSecretFalseEmpty() {
        var dto = new TenantSettingsDto(
                TENANT_ID, "OIDC", false, Set.of(), "Blog", null, null, null, null, null, "", null, null, null, null,
                false, Map.of());
        assertThat(dto.hasOidcClientSecret()).isFalse();
    }
}
