package de.tomsblog.tenantmanagement.adapter.outbound.persistence;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.tenantmanagement.domain.model.LoginMode;
import de.tomsblog.tenantmanagement.domain.model.Tenant;
import de.tomsblog.tenantmanagement.domain.model.TenantStatus;
import java.util.HashSet;

/**
 * Mapper between domain Tenant and JPA entity.
 *
 * @req SWR-072
 */
public final class TenantMapper {

    private TenantMapper() {}

    public static TenantJpaEntity toEntity(Tenant tenant) {
        var entity = new TenantJpaEntity();
        entity.setTenantId(tenant.getTenantId().value());
        entity.setSlug(tenant.getSlug());
        entity.setDisplayName(tenant.getDisplayName());
        entity.setTagline(tenant.getTagline());
        entity.setStatus(tenant.getStatus().name());
        entity.setLoginMode(tenant.getLoginMode().name());
        entity.setAutoApproveOidc(tenant.isAutoApproveOidc());
        entity.setAutoApproveEmailDomains(new HashSet<>(tenant.getAutoApproveEmailDomains()));
        entity.setImpressumContent(tenant.getImpressumContent());
        entity.setPrivacyPolicyContent(tenant.getPrivacyPolicyContent());
        entity.setOidcIssuerUrl(tenant.getOidcIssuerUrl());
        entity.setOidcClientId(tenant.getOidcClientId());
        entity.setOidcClientSecret(tenant.getOidcClientSecret());
        return entity;
    }

    public static Tenant toDomain(TenantJpaEntity entity) {
        return Tenant.reconstitute(
                TenantId.of(entity.getTenantId()),
                entity.getSlug(),
                entity.getDisplayName(),
                entity.getTagline(),
                TenantStatus.valueOf(entity.getStatus()),
                LoginMode.valueOf(entity.getLoginMode()),
                entity.isAutoApproveOidc(),
                new HashSet<>(entity.getAutoApproveEmailDomains()),
                entity.getImpressumContent(),
                entity.getPrivacyPolicyContent(),
                entity.getOidcIssuerUrl(),
                entity.getOidcClientId(),
                entity.getOidcClientSecret());
    }
}
