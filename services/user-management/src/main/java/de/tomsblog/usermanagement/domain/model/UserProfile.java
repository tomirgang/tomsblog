package de.tomsblog.usermanagement.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Aggregate root representing a user profile with roles and tenant memberships.
 *
 * @req SWR-043
 * @req SWR-007
 */
public class UserProfile {

    private final UserProfileId id;
    private final String oidcSubject;
    private final String username;
    private String passwordHash;
    private final AuthSource authSource;
    private String email;
    private String displayName;
    private ApprovalStatus approvalStatus;
    private final Set<Role> globalRoles;
    private final Set<TenantMembership> tenantMemberships;
    private final Instant createdAt;
    private Instant lastLoginAt;

    private UserProfile(
            UserProfileId id,
            String oidcSubject,
            String username,
            String passwordHash,
            AuthSource authSource,
            String email,
            String displayName,
            ApprovalStatus approvalStatus,
            Set<Role> globalRoles,
            Set<TenantMembership> tenantMemberships,
            Instant createdAt,
            Instant lastLoginAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.oidcSubject = oidcSubject;
        this.username = username;
        this.passwordHash = passwordHash;
        this.authSource = Objects.requireNonNull(authSource, "authSource must not be null");
        this.email = email;
        this.displayName = displayName;
        this.approvalStatus = Objects.requireNonNull(approvalStatus, "approvalStatus must not be null");
        this.globalRoles = EnumSet.noneOf(Role.class);
        this.globalRoles.addAll(globalRoles);
        this.tenantMemberships = new HashSet<>(tenantMemberships);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.lastLoginAt = lastLoginAt;
    }

    /**
     * Creates a new OIDC-based user profile with PENDING approval status.
     */
    public static UserProfile createFromOidc(String oidcSubject, String email, String displayName) {
        Objects.requireNonNull(oidcSubject, "oidcSubject must not be null");
        return new UserProfile(
                UserProfileId.generate(),
                oidcSubject,
                null,
                null,
                AuthSource.OIDC,
                email,
                displayName,
                ApprovalStatus.PENDING,
                Set.of(Role.READER),
                Set.of(),
                Instant.now(),
                Instant.now());
    }

    /**
     * Creates a new internal user profile with PENDING approval status.
     */
    public static UserProfile createInternal(String username, String passwordHash, String email, String displayName) {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        return new UserProfile(
                UserProfileId.generate(),
                null,
                username,
                passwordHash,
                AuthSource.INTERNAL,
                email,
                displayName,
                ApprovalStatus.PENDING,
                Set.of(Role.READER),
                Set.of(),
                Instant.now(),
                Instant.now());
    }

    /**
     * Reconstitutes a user profile from persistence.
     */
    public static UserProfile reconstitute(
            UserProfileId id,
            String oidcSubject,
            String username,
            String passwordHash,
            AuthSource authSource,
            String email,
            String displayName,
            ApprovalStatus approvalStatus,
            Set<Role> globalRoles,
            Set<TenantMembership> tenantMemberships,
            Instant createdAt,
            Instant lastLoginAt) {
        return new UserProfile(
                id,
                oidcSubject,
                username,
                passwordHash,
                authSource,
                email,
                displayName,
                approvalStatus,
                globalRoles,
                tenantMemberships,
                createdAt,
                lastLoginAt);
    }

    public void syncFromOidc(String email, String displayName) {
        this.email = email;
        this.displayName = displayName;
        this.lastLoginAt = Instant.now();
    }

    public void syncFromInternal(String email, String displayName) {
        this.email = email;
        this.displayName = displayName;
        this.lastLoginAt = Instant.now();
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
    }

    public void approve() {
        this.approvalStatus = ApprovalStatus.APPROVED;
    }

    public void reject() {
        this.approvalStatus = ApprovalStatus.REJECTED;
    }

    public void assignGlobalRole(Role role) {
        Objects.requireNonNull(role, "role must not be null");
        globalRoles.add(role);
    }

    public void removeGlobalRole(Role role) {
        Objects.requireNonNull(role, "role must not be null");
        globalRoles.remove(role);
    }

    public void addTenantMembership(TenantMembership membership) {
        Objects.requireNonNull(membership, "membership must not be null");
        tenantMemberships.removeIf(m -> m.tenantId().equals(membership.tenantId()));
        tenantMemberships.add(membership);
    }

    public void removeTenantMembership(de.tomsblog.shared.tenant.TenantId tenantId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        tenantMemberships.removeIf(m -> m.tenantId().equals(tenantId));
    }

    /**
     * Returns the effective roles for a given tenant. If the user is not approved,
     * only READER is returned. Otherwise, global roles are combined with the
     * tenant-specific role.
     */
    public Set<Role> getEffectiveRoles(de.tomsblog.shared.tenant.TenantId tenantId) {
        if (approvalStatus != ApprovalStatus.APPROVED) {
            return Set.of(Role.READER);
        }

        Set<Role> effective = EnumSet.noneOf(Role.class);
        effective.addAll(globalRoles);

        tenantMemberships.stream()
                .filter(m -> m.tenantId().equals(tenantId))
                .findFirst()
                .ifPresent(m -> effective.add(m.role()));

        if (effective.isEmpty()) {
            effective.add(Role.READER);
        }

        return Collections.unmodifiableSet(effective);
    }

    public boolean isPending() {
        return approvalStatus == ApprovalStatus.PENDING;
    }

    public boolean isApproved() {
        return approvalStatus == ApprovalStatus.APPROVED;
    }

    public UserProfileId getId() {
        return id;
    }

    public String getOidcSubject() {
        return oidcSubject;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AuthSource getAuthSource() {
        return authSource;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public Set<Role> getGlobalRoles() {
        return Collections.unmodifiableSet(globalRoles);
    }

    public Set<TenantMembership> getTenantMemberships() {
        return Collections.unmodifiableSet(tenantMemberships);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
}
