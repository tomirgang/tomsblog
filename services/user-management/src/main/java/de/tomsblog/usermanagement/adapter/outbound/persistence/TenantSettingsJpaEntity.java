package de.tomsblog.usermanagement.adapter.outbound.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * JPA entity for persisting tenant settings.
 *
 * @req SWR-044
 * @req SWR-045
 * @req SWR-061
 */
@Entity
@Table(name = "tenant_settings")
public class TenantSettingsJpaEntity {

    @Id
    @Column(name = "tenant_id", updatable = false)
    private UUID tenantId;

    @Column(name = "login_mode", nullable = false)
    private String loginMode;

    @Column(name = "auto_approve_oidc", nullable = false)
    private boolean autoApproveOidc;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tenant_auto_approve_domains", joinColumns = @JoinColumn(name = "tenant_id"))
    @Column(name = "domain")
    private Set<String> autoApproveEmailDomains = new HashSet<>();

    @Column(name = "display_name", nullable = false)
    private String displayName = "Toms Blog";

    @Column(name = "tagline")
    private String tagline;

    @Column(name = "impressum_content", columnDefinition = "TEXT")
    private String impressumContent;

    @Column(name = "privacy_policy_content", columnDefinition = "TEXT")
    private String privacyPolicyContent;

    @Column(name = "oidc_issuer_url")
    private String oidcIssuerUrl;

    @Column(name = "oidc_client_id")
    private String oidcClientId;

    @Column(name = "oidc_client_secret")
    private String oidcClientSecret;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TenantSettingsJpaEntity() {
        // JPA
    }

    @PrePersist
    void prePersist() {
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getLoginMode() {
        return loginMode;
    }

    public void setLoginMode(String loginMode) {
        this.loginMode = loginMode;
    }

    public boolean isAutoApproveOidc() {
        return autoApproveOidc;
    }

    public void setAutoApproveOidc(boolean autoApproveOidc) {
        this.autoApproveOidc = autoApproveOidc;
    }

    public Set<String> getAutoApproveEmailDomains() {
        return autoApproveEmailDomains;
    }

    public void setAutoApproveEmailDomains(Set<String> autoApproveEmailDomains) {
        this.autoApproveEmailDomains = autoApproveEmailDomains;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getImpressumContent() {
        return impressumContent;
    }

    public void setImpressumContent(String impressumContent) {
        this.impressumContent = impressumContent;
    }

    public String getPrivacyPolicyContent() {
        return privacyPolicyContent;
    }

    public void setPrivacyPolicyContent(String privacyPolicyContent) {
        this.privacyPolicyContent = privacyPolicyContent;
    }

    public String getOidcIssuerUrl() {
        return oidcIssuerUrl;
    }

    public void setOidcIssuerUrl(String oidcIssuerUrl) {
        this.oidcIssuerUrl = oidcIssuerUrl;
    }

    public String getOidcClientId() {
        return oidcClientId;
    }

    public void setOidcClientId(String oidcClientId) {
        this.oidcClientId = oidcClientId;
    }

    public String getOidcClientSecret() {
        return oidcClientSecret;
    }

    public void setOidcClientSecret(String oidcClientSecret) {
        this.oidcClientSecret = oidcClientSecret;
    }
}
