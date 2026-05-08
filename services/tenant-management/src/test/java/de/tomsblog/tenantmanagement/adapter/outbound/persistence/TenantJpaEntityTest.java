package de.tomsblog.tenantmanagement.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TenantJpaEntityTest {

    @Test
    @DisplayName("SWR-072: entity fields are accessible via getters/setters")
    void fieldsAccessible() {
        var entity = new TenantJpaEntity();
        var id = UUID.randomUUID();

        entity.setTenantId(id);
        entity.setSlug("test");
        entity.setDisplayName("Test");
        entity.setTagline("tag");
        entity.setStatus("ACTIVE");
        entity.setLoginMode("BOTH");
        entity.setAutoApproveOidc(true);
        entity.setAutoApproveEmailDomains(Set.of("x.com"));
        entity.setImpressumContent("imp");
        entity.setPrivacyPolicyContent("priv");
        entity.setOidcIssuerUrl("https://iss");
        entity.setOidcClientId("c");
        entity.setOidcClientSecret("s");

        assertThat(entity.getTenantId()).isEqualTo(id);
        assertThat(entity.getSlug()).isEqualTo("test");
        assertThat(entity.getDisplayName()).isEqualTo("Test");
        assertThat(entity.getTagline()).isEqualTo("tag");
        assertThat(entity.getStatus()).isEqualTo("ACTIVE");
        assertThat(entity.getLoginMode()).isEqualTo("BOTH");
        assertThat(entity.isAutoApproveOidc()).isTrue();
        assertThat(entity.getAutoApproveEmailDomains()).containsExactly("x.com");
        assertThat(entity.getImpressumContent()).isEqualTo("imp");
        assertThat(entity.getPrivacyPolicyContent()).isEqualTo("priv");
        assertThat(entity.getOidcIssuerUrl()).isEqualTo("https://iss");
        assertThat(entity.getOidcClientId()).isEqualTo("c");
        assertThat(entity.getOidcClientSecret()).isEqualTo("s");
    }

    @Test
    @DisplayName("SWR-072: prePersist sets timestamps")
    void prePersistSetsTimestamps() {
        var entity = new TenantJpaEntity();
        entity.prePersist();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("SWR-072: preUpdate updates timestamp")
    void preUpdateUpdatesTimestamp() {
        var entity = new TenantJpaEntity();
        entity.prePersist();
        var firstUpdate = entity.getUpdatedAt();
        entity.preUpdate();
        assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(firstUpdate);
    }

    @Test
    @DisplayName("SWR-072: createdAt and updatedAt setters work")
    void timestampSetters() {
        var entity = new TenantJpaEntity();
        var now = java.time.Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
    }
}
