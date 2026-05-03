package de.tomsblog.usermanagement.adapter.outbound.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for user profiles.
 */
public interface SpringDataUserProfileRepository extends JpaRepository<UserProfileJpaEntity, UUID> {

    Optional<UserProfileJpaEntity> findByOidcSubject(String oidcSubject);

    Optional<UserProfileJpaEntity> findByUsername(String username);
}
