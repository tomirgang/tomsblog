package de.tomsblog.usermanagement.adapter.outbound.persistence;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import java.util.HashSet;

/**
 * Mapper between domain TenantSettings and JPA entity.
 */
public final class TenantSettingsMapper {

    private TenantSettingsMapper() {}

    public static TenantSettingsJpaEntity toEntity(TenantSettings settings) {
        var entity = new TenantSettingsJpaEntity();
        entity.setTenantId(settings.getTenantId().value());
        entity.setLoginMode(settings.getLoginMode().name());
        entity.setAutoApproveOidc(settings.isAutoApproveOidc());
        entity.setAutoApproveEmailDomains(new HashSet<>(settings.getAutoApproveEmailDomains()));
        return entity;
    }

    public static TenantSettings toDomain(TenantSettingsJpaEntity entity) {
        return TenantSettings.reconstitute(
                TenantId.of(entity.getTenantId()),
                LoginMode.valueOf(entity.getLoginMode()),
                entity.isAutoApproveOidc(),
                new HashSet<>(entity.getAutoApproveEmailDomains()));
    }
}
