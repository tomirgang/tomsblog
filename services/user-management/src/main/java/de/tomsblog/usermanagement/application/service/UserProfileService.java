package de.tomsblog.usermanagement.application.service;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.SyncInternalUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.SyncOidcUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.UserProfileUseCase;
import de.tomsblog.usermanagement.application.port.outbound.UserProfileRepository;
import de.tomsblog.usermanagement.domain.model.Role;
import de.tomsblog.usermanagement.domain.model.TenantMembership;
import de.tomsblog.usermanagement.domain.model.UserProfile;

/**
 * Application service implementing user profile use cases.
 *
 * @req SWR-043
 * @req SWR-007
 */
public class UserProfileService implements UserProfileUseCase {

    private final UserProfileRepository repository;

    public UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserProfile syncFromOidc(SyncOidcUserCommand command) {
        var existing = repository.findByOidcSubject(command.oidcSubject());

        if (existing.isPresent()) {
            var profile = existing.get();
            profile.syncFromOidc(command.email(), command.displayName());
            return repository.save(profile);
        }

        var profile = UserProfile.createFromOidc(command.oidcSubject(), command.email(), command.displayName());
        return repository.save(profile);
    }

    @Override
    public UserProfile syncFromInternal(SyncInternalUserCommand command) {
        var existing = repository.findByUsername(command.username());

        if (existing.isPresent()) {
            var profile = existing.get();
            profile.syncFromInternal(command.email(), command.displayName());
            profile.updatePasswordHash(command.passwordHash());
            return repository.save(profile);
        }

        var profile = UserProfile.createInternal(
                command.username(), command.passwordHash(), command.email(), command.displayName());
        return repository.save(profile);
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
    }

    @Override
    public void rejectUser(String identifier) {
        var profile = findProfile(identifier);
        profile.reject();
        repository.save(profile);
    }

    @Override
    public void assignGlobalRole(String identifier, Role role) {
        var profile = findProfile(identifier);
        profile.assignGlobalRole(role);
        repository.save(profile);
    }

    @Override
    public void removeGlobalRole(String identifier, Role role) {
        var profile = findProfile(identifier);
        profile.removeGlobalRole(role);
        repository.save(profile);
    }

    @Override
    public void addTenantMembership(String identifier, TenantId tenantId, Role role) {
        var profile = findProfile(identifier);
        profile.addTenantMembership(new TenantMembership(tenantId, role));
        repository.save(profile);
    }

    @Override
    public void removeTenantMembership(String identifier, TenantId tenantId) {
        var profile = findProfile(identifier);
        profile.removeTenantMembership(tenantId);
        repository.save(profile);
    }

    private UserProfile findProfile(String identifier) {
        return repository
                .findByOidcSubject(identifier)
                .or(() -> repository.findByUsername(identifier))
                .orElseThrow(() -> new UserProfileNotFoundException(identifier));
    }
}
