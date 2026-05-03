package de.tomsblog.blogcontent.application.port.outbound;

import de.tomsblog.blogcontent.domain.model.Post;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.Slug;
import de.tomsblog.shared.tenant.TenantId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for post persistence.
 *
 * @req SWR-001
 * @req SWR-003
 * @req SWR-026
 * @req SWR-033
 * @req SWR-038
 * @req SWR-039
 * @req SWR-042
 */
public interface PostRepository {

    Post save(Post post);

    Optional<Post> findByIdAndTenantId(PostId id, TenantId tenantId);

    List<Post> findAllByTenantId(TenantId tenantId);

    /** @req SWR-026 */
    List<Post> findPublishedByTenantId(TenantId tenantId);

    /** @req SWR-033 */
    List<Post> findRecentPublishedByTenantId(TenantId tenantId, int limit);

    /** @req SWR-026 */
    Optional<Post> findBySlugAndTenantId(Slug slug, TenantId tenantId);

    void deleteByIdAndTenantId(PostId id, TenantId tenantId);

    /** @req SWR-038 */
    List<Post> searchPublished(String query, TenantId tenantId);

    /** @req SWR-039 */
    Optional<Post> findPreviousPublished(TenantId tenantId, Instant publishedAt);

    /** @req SWR-039 */
    Optional<Post> findNextPublished(TenantId tenantId, Instant publishedAt);

    /** @req SWR-042 */
    List<Post> findFeaturedByTenantId(TenantId tenantId, LocalDate today);
}
