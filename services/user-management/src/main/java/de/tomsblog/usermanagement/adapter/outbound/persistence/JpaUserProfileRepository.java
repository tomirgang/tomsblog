package de.tomsblog.usermanagement.adapter.outbound.persistence;

import de.tomsblog.usermanagement.application.port.outbound.UserProfileRepository;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA adapter implementing the user profile repository port.
 *
 * @req SWR-043
 */
@Repository
@Transactional
public class JpaUserProfileRepository implements UserProfileRepository {

    private final SpringDataUserProfileRepository springDataRepository;

    public JpaUserProfileRepository(SpringDataUserProfileRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public UserProfile save(UserProfile userProfile) {
        var entity = UserProfileMapper.toEntity(userProfile);
        var saved = springDataRepository.save(entity);
        return UserProfileMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfile> findByOidcSubject(String oidcSubject) {
        return springDataRepository.findByOidcSubject(oidcSubject).map(UserProfileMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfile> findByUsername(String username) {
        return springDataRepository.findByUsername(username).map(UserProfileMapper::toDomain);
    }
}
