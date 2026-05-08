package de.tomsblog.usermanagement.application.service;

import de.tomsblog.shared.audit.AuditLogEntry;
import de.tomsblog.shared.audit.AuditLogger;
import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.RegisterUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.SyncInternalUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.SyncOidcUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.UserProfileUseCase;
import de.tomsblog.usermanagement.application.port.outbound.TenantSettingsRepository;
import de.tomsblog.usermanagement.application.port.outbound.UserProfileRepository;
import de.tomsblog.usermanagement.domain.model.AuthSource;
import de.tomsblog.usermanagement.domain.model.Role;
import de.tomsblog.usermanagement.domain.model.TenantMembership;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Application service implementing user profile use cases.
 *
 * @req SWR-043
 * @req SWR-007
 * @req SWR-045
 * @req SWR-056
 * @req SWR-059
 */
public class UserProfileService implements UserProfileUseCase {

    private final UserProfileRepository repository;
    private final TenantSettingsRepository tenantSettingsRepository;
    private final AuditLogger auditLogger;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(
            UserProfileRepository repository,
            TenantSettingsRepository tenantSettingsRepository,
            AuditLogger auditLogger,
            PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.tenantSettingsRepository = tenantSettingsRepository;
        this.auditLogger = auditLogger;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserProfile syncFromOidc(SyncOidcUserCommand command) {
        var existing = repository.findByOidcSubject(command.oidcSubject());

        if (existing.isPresent()) {
            var profile = existing.get();
            profile.syncFromOidc(command.email(), command.displayName());
            UserProfile saved = repository.save(profile);
            auditLogger.log(AuditLogEntry.create(
                    command.tenantId().toString(),
                    "system",
                    "USER_SYNCED_OIDC",
                    "UserProfile",
                    saved.getId().asString()));
            return saved;
        }

        var profile = UserProfile.createFromOidc(command.oidcSubject(), command.email(), command.displayName());
        profile.addTenantMembership(new TenantMembership(command.tenantId(), Role.READER));
        applyAutoApproval(profile, command.tenantId(), AuthSource.OIDC, command.email());
        UserProfile saved = repository.save(profile);
        auditLogger.log(AuditLogEntry.create(
                command.tenantId().toString(),
                "system",
                "USER_SYNCED_OIDC",
                "UserProfile",
                saved.getId().asString()));
        return saved;
    }

    @Override
    public UserProfile syncFromInternal(SyncInternalUserCommand command) {
        var existing = repository.findByUsername(command.username());

        if (existing.isPresent()) {
            var profile = existing.get();
            profile.syncFromInternal(command.email(), command.displayName());
            profile.updatePasswordHash(command.passwordHash());
            UserProfile saved = repository.save(profile);
            auditLogger.log(AuditLogEntry.create(
                    command.tenantId().toString(),
                    "system",
                    "USER_SYNCED_INTERNAL",
                    "UserProfile",
                    saved.getId().asString()));
            return saved;
        }

        var profile = UserProfile.createInternal(
                command.username(), command.passwordHash(), command.email(), command.displayName());
        profile.addTenantMembership(new TenantMembership(command.tenantId(), Role.READER));
        applyAutoApproval(profile, command.tenantId(), AuthSource.INTERNAL, command.email());
        UserProfile saved = repository.save(profile);
        auditLogger.log(AuditLogEntry.create(
                command.tenantId().toString(),
                "system",
                "USER_SYNCED_INTERNAL",
                "UserProfile",
                saved.getId().asString()));
        return saved;
    }

    @Override
    public UserProfile register(RegisterUserCommand command) {
        if (repository.existsByUsername(command.username())) {
            throw new UserAlreadyExistsException("Username already taken: " + command.username());
        }
        if (repository.existsByEmail(command.email())) {
            throw new UserAlreadyExistsException("Email already registered: " + command.email());
        }

        String hashedPassword = passwordEncoder.encode(command.password());
        var profile =
                UserProfile.createInternal(command.username(), hashedPassword, command.email(), command.displayName());
        profile.addTenantMembership(new TenantMembership(command.tenantId(), Role.READER));
        applyAutoApproval(profile, command.tenantId(), AuthSource.INTERNAL, command.email());
        UserProfile saved = repository.save(profile);
        auditLogger.log(AuditLogEntry.create(
                command.tenantId().toString(),
                command.username(),
                "USER_REGISTERED",
                "UserProfile",
                saved.getId().asString()));
        return saved;
    }

    private void applyAutoApproval(UserProfile profile, TenantId tenantId, AuthSource authSource, String email) {
        var settings =
                tenantSettingsRepository.findByTenantId(tenantId).orElseGet(() -> TenantSettings.create(tenantId));
        if (settings.shouldAutoApprove(authSource, email)) {
            profile.approve();
        }
    }

    @Override
    public UserProfile findByOidcSubject(String oidcSubject) {
        return repository
                .findByOidcSubject(oidcSubject)
                .orElseThrow(() -> new UserProfileNotFoundException(oidcSubject));
    }

    @Override
    public UserProfile findByUsername(String username) {
        return repository.findByUsername(username).orElseThrow(() -> new UserProfileNotFoundException(username));
    }

    @Override
    public void approveUser(String identifier) {
        var profile = findProfile(identifier);
        profile.approve();
        repository.save(profile);
        auditLogger.log(AuditLogEntry.create(
                null, "system", "USER_APPROVED", "UserProfile", profile.getId().asString()));
    }

    @Override
    public void rejectUser(String identifier) {
        var profile = findProfile(identifier);
        profile.reject();
        repository.save(profile);
        auditLogger.log(AuditLogEntry.create(
                null, "system", "USER_REJECTED", "UserProfile", profile.getId().asString()));
    }

    @Override
    public void assignGlobalRole(String identifier, Role role) {
        var profile = findProfile(identifier);
        profile.assignGlobalRole(role);
        repository.save(profile);
        auditLogger.log(AuditLogEntry.create(
                null,
                "system",
                "GLOBAL_ROLE_ASSIGNED",
                "UserProfile",
                profile.getId().asString(),
                role.name()));
    }

    @Override
    public void removeGlobalRole(String identifier, Role role) {
        var profile = findProfile(identifier);
        profile.removeGlobalRole(role);
        repository.save(profile);
        auditLogger.log(AuditLogEntry.create(
                null,
                "system",
                "GLOBAL_ROLE_REMOVED",
                "UserProfile",
                profile.getId().asString(),
                role.name()));
    }

    @Override
    public void addTenantMembership(String identifier, TenantId tenantId, Role role) {
        var profile = findProfile(identifier);
        profile.addTenantMembership(new TenantMembership(tenantId, role));
        repository.save(profile);
        auditLogger.log(AuditLogEntry.create(
                tenantId.toString(),
                "system",
                "TENANT_MEMBERSHIP_ADDED",
                "UserProfile",
                profile.getId().asString(),
                role.name()));
    }

    @Override
    public void removeTenantMembership(String identifier, TenantId tenantId) {
        var profile = findProfile(identifier);
        profile.removeTenantMembership(tenantId);
        repository.save(profile);
        auditLogger.log(AuditLogEntry.create(
                tenantId.toString(),
                "system",
                "TENANT_MEMBERSHIP_REMOVED",
                "UserProfile",
                profile.getId().asString()));
    }

    @Override
    public List<UserProfile> listByTenantId(TenantId tenantId) {
        return repository.findByTenantId(tenantId);
    }

    private UserProfile findProfile(String identifier) {
        return repository
                .findByOidcSubject(identifier)
                .or(() -> repository.findByUsername(identifier))
                .orElseThrow(() -> new UserProfileNotFoundException(identifier));
    }
}
