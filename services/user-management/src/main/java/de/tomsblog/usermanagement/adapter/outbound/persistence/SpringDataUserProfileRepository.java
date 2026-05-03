package de.tomsblog.usermanagement.adapter.outbound.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for user profiles.
 */
public interface SpringDataUserProfileRepository extends JpaRepository<UserProfileJpaEntity, UUID> {

    Optional<UserProfileJpaEntity> findByOidcSubject(String oidcSubject);

    Optional<UserProfileJpaEntity> findByUsername(String username);

    @Query("SELECT DISTINCT u FROM UserProfileJpaEntity u JOIN u.tenantMemberships m WHERE m.tenantId = :tenantId")
    List<UserProfileJpaEntity> findByTenantId(@Param("tenantId") UUID tenantId);
}
