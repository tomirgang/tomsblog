package de.tomsblog.tenantmanagement.domain.model;

import de.tomsblog.shared.tenant.TenantId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Aggregate root for tenant lifecycle and configuration (ADR-0031).
 *
 * @req SWR-044
 * @req SWR-045
 * @req SWR-050
 * @req SWR-061
 * @req SWR-072
 */
public class Tenant {

    private final TenantId tenantId;
    private String slug;
    private String displayName;
    private String tagline;
    private TenantStatus status;
    private LoginMode loginMode;
    private boolean autoApproveOidc;
    private final Set<String> autoApproveEmailDomains;
    private String impressumContent;
    private String privacyPolicyContent;
    private String oidcIssuerUrl;
    private String oidcClientId;
    private String oidcClientSecret;

    private Tenant(
            TenantId tenantId,
            String slug,
            String displayName,
            String tagline,
            TenantStatus status,
            LoginMode loginMode,
            boolean autoApproveOidc,
            Set<String> autoApproveEmailDomains,
            String impressumContent,
            String privacyPolicyContent,
            String oidcIssuerUrl,
            String oidcClientId,
            String oidcClientSecret) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.slug = Objects.requireNonNull(slug, "slug must not be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
        this.tagline = tagline;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.loginMode = Objects.requireNonNull(loginMode, "loginMode must not be null");
        this.autoApproveOidc = autoApproveOidc;
        this.autoApproveEmailDomains = new HashSet<>(autoApproveEmailDomains);
        this.impressumContent = impressumContent;
        this.privacyPolicyContent = privacyPolicyContent;
        this.oidcIssuerUrl = oidcIssuerUrl;
        this.oidcClientId = oidcClientId;
        this.oidcClientSecret = oidcClientSecret;
    }

    public static Tenant create(TenantId tenantId, String slug, String displayName) {
        return new Tenant(
                tenantId,
                slug,
                displayName,
                null,
                TenantStatus.ACTIVE,
                LoginMode.BOTH,
                false,
                Set.of(),
                null,
                null,
                null,
                null,
                null);
    }

    public static Tenant reconstitute(
            TenantId tenantId,
            String slug,
            String displayName,
            String tagline,
            TenantStatus status,
            LoginMode loginMode,
            boolean autoApproveOidc,
            Set<String> autoApproveEmailDomains,
            String impressumContent,
            String privacyPolicyContent,
            String oidcIssuerUrl,
            String oidcClientId,
            String oidcClientSecret) {
        return new Tenant(
                tenantId,
                slug,
                displayName,
                tagline,
                status,
                loginMode,
                autoApproveOidc,
                autoApproveEmailDomains,
                impressumContent,
                privacyPolicyContent,
                oidcIssuerUrl,
                oidcClientId,
                oidcClientSecret);
    }

    public void updateGeneralSettings(
            String displayName, String tagline, LoginMode loginMode, boolean autoApproveOidc, Set<String> domains) {
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
        this.tagline = tagline;
        this.loginMode = Objects.requireNonNull(loginMode, "loginMode must not be null");
        this.autoApproveOidc = autoApproveOidc;
        this.autoApproveEmailDomains.clear();
        domains.forEach(d -> autoApproveEmailDomains.add(d.toLowerCase()));
    }

    public void updateOidcSettings(String oidcIssuerUrl, String oidcClientId, String oidcClientSecret) {
        this.oidcIssuerUrl = oidcIssuerUrl;
        this.oidcClientId = oidcClientId;
        if (oidcClientSecret != null && !oidcClientSecret.isEmpty() && !"***".equals(oidcClientSecret)) {
            this.oidcClientSecret = oidcClientSecret;
        }
    }

    public void updateLegalSettings(String impressumContent, String privacyPolicyContent) {
        this.impressumContent = impressumContent;
        this.privacyPolicyContent = privacyPolicyContent;
    }

    public void activate() {
        this.status = TenantStatus.ACTIVE;
    }

    public void suspend() {
        this.status = TenantStatus.SUSPENDED;
    }

    public void deactivate() {
        this.status = TenantStatus.DEACTIVATED;
    }

    public boolean shouldAutoApprove(String authSource, String email) {
        if ("OIDC".equals(authSource) && autoApproveOidc) {
            return true;
        }
        if (email != null && !autoApproveEmailDomains.isEmpty()) {
            String domain = extractDomain(email);
            return domain != null && autoApproveEmailDomains.contains(domain.toLowerCase());
        }
        return false;
    }

    private static String extractDomain(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex < 0 || atIndex == email.length() - 1) {
            return null;
        }
        return email.substring(atIndex + 1);
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public String getSlug() {
        return slug;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTagline() {
        return tagline;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public LoginMode getLoginMode() {
        return loginMode;
    }

    public boolean isAutoApproveOidc() {
        return autoApproveOidc;
    }

    public Set<String> getAutoApproveEmailDomains() {
        return Collections.unmodifiableSet(autoApproveEmailDomains);
    }

    public String getImpressumContent() {
        return impressumContent;
    }

    public String getPrivacyPolicyContent() {
        return privacyPolicyContent;
    }

    public String getOidcIssuerUrl() {
        return oidcIssuerUrl;
    }

    public String getOidcClientId() {
        return oidcClientId;
    }

    public String getOidcClientSecret() {
        return oidcClientSecret;
    }
}
