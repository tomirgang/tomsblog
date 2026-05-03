package de.tomsblog.blogcontent.adapter.outbound.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataPostRepository extends JpaRepository<PostJpaEntity, UUID> {

    Optional<PostJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<PostJpaEntity> findAllByTenantId(UUID tenantId);

    List<PostJpaEntity> findAllByTenantIdAndStatusOrderByPublishedAtDesc(UUID tenantId, PostStatusJpa status);

    List<PostJpaEntity> findAllByTenantIdAndStatusOrderByPublishedAtDesc(
            UUID tenantId, PostStatusJpa status, Pageable pageable);

    Optional<PostJpaEntity> findBySlugAndTenantId(String slug, UUID tenantId);

    void deleteByIdAndTenantId(UUID id, UUID tenantId);

    /** @req SWR-038 */
    @Query(
            value = "SELECT * FROM posts WHERE tenant_id = :tenantId AND status = 'PUBLISHED'"
                    + " AND to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content, ''))"
                    + " @@ plainto_tsquery('simple', :query)"
                    + " ORDER BY ts_rank(to_tsvector('simple', coalesce(title, '') || ' '"
                    + " || coalesce(content, '')), plainto_tsquery('simple', :query)) DESC",
            nativeQuery = true)
    List<PostJpaEntity> searchPublished(@Param("query") String query, @Param("tenantId") UUID tenantId);

    /** @req SWR-039 */
    Optional<PostJpaEntity> findFirstByTenantIdAndStatusAndPublishedAtBeforeOrderByPublishedAtDesc(
            UUID tenantId, PostStatusJpa status, Instant publishedAt);

    /** @req SWR-039 */
    Optional<PostJpaEntity> findFirstByTenantIdAndStatusAndPublishedAtAfterOrderByPublishedAtAsc(
            UUID tenantId, PostStatusJpa status, Instant publishedAt);

    /** @req SWR-042 */
    @Query("SELECT p FROM PostJpaEntity p WHERE p.tenantId = :tenantId AND p.status = 'PUBLISHED'"
            + " AND p.featuredFrom <= :today AND p.featuredUntil >= :today"
            + " ORDER BY p.featuredFrom DESC")
    List<PostJpaEntity> findFeaturedByTenantId(@Param("tenantId") UUID tenantId, @Param("today") LocalDate today);
}
