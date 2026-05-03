package de.tomsblog.usermanagement.application.port.outbound;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for user profile persistence.
 *
 * @req SWR-043
 * @req SWR-051
 */
public interface UserProfileRepository {

    UserProfile save(UserProfile userProfile);

    Optional<UserProfile> findByOidcSubject(String oidcSubject);

    Optional<UserProfile> findByUsername(String username);

    List<UserProfile> findByTenantId(TenantId tenantId);
}
