package de.tomsblog.usermanagement.adapter.outbound.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * JPA entity for persisting user profiles.
 *
 * @req SWR-043
 */
@Entity
@Table(name = "user_profiles")
public class UserProfileJpaEntity {

    @Id
    @Column(updatable = false)
    private UUID id;

    @Column(name = "oidc_subject", unique = true)
    private String oidcSubject;

    @Column(unique = true)
    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "auth_source", nullable = false)
    private String authSource;

    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "approval_status", nullable = false)
    private String approvalStatus;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_global_roles", joinColumns = @JoinColumn(name = "user_profile_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<RoleJpa> globalRoles = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_tenant_memberships", joinColumns = @JoinColumn(name = "user_profile_id"))
    private Set<TenantMembershipEmbeddable> tenantMemberships = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected UserProfileJpaEntity() {
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOidcSubject() {
        return oidcSubject;
    }

    public void setOidcSubject(String oidcSubject) {
        this.oidcSubject = oidcSubject;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getAuthSource() {
        return authSource;
    }

    public void setAuthSource(String authSource) {
        this.authSource = authSource;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public Set<RoleJpa> getGlobalRoles() {
        return globalRoles;
    }

    public void setGlobalRoles(Set<RoleJpa> globalRoles) {
        this.globalRoles = globalRoles;
    }

    public Set<TenantMembershipEmbeddable> getTenantMemberships() {
        return tenantMemberships;
    }

    public void setTenantMemberships(Set<TenantMembershipEmbeddable> tenantMemberships) {
        this.tenantMemberships = tenantMemberships;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
