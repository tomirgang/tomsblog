package de.tomsblog.usermanagement.domain.model;

import de.tomsblog.shared.tenant.TenantId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Value object representing tenant-specific settings including login mode, auto-approval rules,
 * and branding.
 *
 * @req SWR-044
 * @req SWR-045
 * @req SWR-050
 */
public class TenantSettings {

    private final TenantId tenantId;
    private LoginMode loginMode;
    private boolean autoApproveOidc;
    private final Set<String> autoApproveEmailDomains;
    private String displayName;
    private String tagline;
    private String impressumContent;
    private String privacyPolicyContent;

    private TenantSettings(
            TenantId tenantId,
            LoginMode loginMode,
            boolean autoApproveOidc,
            Set<String> autoApproveEmailDomains,
            String displayName,
            String tagline,
            String impressumContent,
            String privacyPolicyContent) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.loginMode = Objects.requireNonNull(loginMode, "loginMode must not be null");
        this.autoApproveOidc = autoApproveOidc;
        this.autoApproveEmailDomains = new HashSet<>(autoApproveEmailDomains);
        this.displayName = displayName;
        this.tagline = tagline;
        this.impressumContent = impressumContent;
        this.privacyPolicyContent = privacyPolicyContent;
    }

    public static TenantSettings create(TenantId tenantId) {
        return new TenantSettings(tenantId, LoginMode.BOTH, false, Set.of(), "Toms Blog", null, null, null);
    }

    public static TenantSettings reconstitute(
            TenantId tenantId,
            LoginMode loginMode,
            boolean autoApproveOidc,
            Set<String> autoApproveEmailDomains,
            String displayName,
            String tagline,
            String impressumContent,
            String privacyPolicyContent) {
        return new TenantSettings(
                tenantId,
                loginMode,
                autoApproveOidc,
                autoApproveEmailDomains,
                displayName,
                tagline,
                impressumContent,
                privacyPolicyContent);
    }

    public void updateLoginMode(LoginMode loginMode) {
        this.loginMode = Objects.requireNonNull(loginMode, "loginMode must not be null");
    }

    public void updateAutoApproveOidc(boolean autoApproveOidc) {
        this.autoApproveOidc = autoApproveOidc;
    }

    public void addAutoApproveEmailDomain(String domain) {
        Objects.requireNonNull(domain, "domain must not be null");
        autoApproveEmailDomains.add(domain.toLowerCase());
    }

    public void removeAutoApproveEmailDomain(String domain) {
        Objects.requireNonNull(domain, "domain must not be null");
        autoApproveEmailDomains.remove(domain.toLowerCase());
    }

    public void setAutoApproveEmailDomains(Set<String> domains) {
        Objects.requireNonNull(domains, "domains must not be null");
        autoApproveEmailDomains.clear();
        domains.forEach(d -> autoApproveEmailDomains.add(d.toLowerCase()));
    }

    /**
     * Checks whether a user with the given auth source and email should be auto-approved.
     */
    public boolean shouldAutoApprove(AuthSource authSource, String email) {
        if (authSource == AuthSource.OIDC && autoApproveOidc) {
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

    public LoginMode getLoginMode() {
        return loginMode;
    }

    public boolean isAutoApproveOidc() {
        return autoApproveOidc;
    }

    public Set<String> getAutoApproveEmailDomains() {
        return Collections.unmodifiableSet(autoApproveEmailDomains);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTagline() {
        return tagline;
    }

    public void updateTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getImpressumContent() {
        return impressumContent;
    }

    public void updateImpressumContent(String impressumContent) {
        this.impressumContent = impressumContent;
    }

    public String getPrivacyPolicyContent() {
        return privacyPolicyContent;
    }

    public void updatePrivacyPolicyContent(String privacyPolicyContent) {
        this.privacyPolicyContent = privacyPolicyContent;
    }
}
