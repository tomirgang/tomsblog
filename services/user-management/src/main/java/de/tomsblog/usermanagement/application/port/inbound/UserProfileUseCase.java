package de.tomsblog.usermanagement.application.port.inbound;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.domain.model.Role;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import java.util.List;

/**
 * Inbound port for user profile management.
 *
 * @req SWR-043
 * @req SWR-051
 */
public interface UserProfileUseCase {

    UserProfile syncFromOidc(SyncOidcUserCommand command);

    UserProfile syncFromInternal(SyncInternalUserCommand command);

    UserProfile findByOidcSubject(String oidcSubject);

    UserProfile findByUsername(String username);

    void approveUser(String identifier);

    void rejectUser(String identifier);

    void assignGlobalRole(String identifier, Role role);

    void removeGlobalRole(String identifier, Role role);

    void addTenantMembership(String identifier, TenantId tenantId, Role role);

    void removeTenantMembership(String identifier, TenantId tenantId);

    List<UserProfile> listByTenantId(TenantId tenantId);
}
