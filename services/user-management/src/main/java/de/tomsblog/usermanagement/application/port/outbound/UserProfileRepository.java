package de.tomsblog.usermanagement.application.port.outbound;

import de.tomsblog.usermanagement.domain.model.UserProfile;
import java.util.Optional;

/**
 * Outbound port for user profile persistence.
 *
 * @req SWR-043
 */
public interface UserProfileRepository {

    UserProfile save(UserProfile userProfile);

    Optional<UserProfile> findByOidcSubject(String oidcSubject);

    Optional<UserProfile> findByUsername(String username);
}
