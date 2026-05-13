package de.tomsblog.usermanagement.adapter.outbound.persistence;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import java.util.HashMap;
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
        entity.setDisplayName(settings.getDisplayName() != null ? settings.getDisplayName() : "Toms Blog");
        entity.setTagline(settings.getTagline());
        entity.setImpressumContent(settings.getImpressumContent());
        entity.setPrivacyPolicyContent(settings.getPrivacyPolicyContent());
        entity.setOidcIssuerUrl(settings.getOidcIssuerUrl());
        entity.setOidcClientId(settings.getOidcClientId());
        entity.setOidcClientSecret(settings.getOidcClientSecret());
        entity.setOidcButtonText(settings.getOidcButtonText());
        entity.setDefaultRole(settings.getDefaultRole() != null ? settings.getDefaultRole() : "READER");
        entity.setLogoUrl(settings.getLogoUrl());
        entity.setFaviconUrl(settings.getFaviconUrl());
        entity.setOidcRoleMappingEnabled(settings.isOidcRoleMappingEnabled());
        entity.setOidcRoleMappings(new HashMap<>(settings.getOidcRoleMappings()));
        return entity;
    }

    public static TenantSettings toDomain(TenantSettingsJpaEntity entity) {
        return TenantSettings.reconstitute(
                TenantId.of(entity.getTenantId()),
                LoginMode.valueOf(entity.getLoginMode()),
                entity.isAutoApproveOidc(),
                new HashSet<>(entity.getAutoApproveEmailDomains()),
                entity.getDisplayName(),
                entity.getTagline(),
                entity.getImpressumContent(),
                entity.getPrivacyPolicyContent(),
                entity.getOidcIssuerUrl(),
                entity.getOidcClientId(),
                entity.getOidcClientSecret(),
                entity.getOidcButtonText(),
                entity.getDefaultRole(),
                entity.getLogoUrl(),
                entity.getFaviconUrl(),
                entity.isOidcRoleMappingEnabled(),
                new HashMap<>(entity.getOidcRoleMappings()));
    }
}
