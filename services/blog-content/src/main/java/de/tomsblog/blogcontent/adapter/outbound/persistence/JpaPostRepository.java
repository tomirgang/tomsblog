package de.tomsblog.blogcontent.adapter.outbound.persistence;

import de.tomsblog.blogcontent.application.port.outbound.PostRepository;
import de.tomsblog.blogcontent.domain.model.Post;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.Slug;
import de.tomsblog.shared.tenant.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA implementation of the post repository port.
 *
 * @req SWR-001
 * @req SWR-003
 * @req SWR-006
 * @req SWR-026
 * @req SWR-038
 * @req SWR-039
 */
@Repository
@Transactional
@SuppressWarnings("null")
public class JpaPostRepository implements PostRepository {

    private final SpringDataPostRepository springDataRepo;

    public JpaPostRepository(SpringDataPostRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Post save(Post post) {
        PostJpaEntity entity = PostMapper.toEntity(post);
        PostJpaEntity saved = springDataRepo.save(entity);
        return PostMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Post> findByIdAndTenantId(PostId id, TenantId tenantId) {
        return springDataRepo.findByIdAndTenantId(id.value(), tenantId.value()).map(PostMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> findAllByTenantId(TenantId tenantId) {
        return springDataRepo.findAllByTenantId(tenantId.value()).stream()
                .map(PostMapper::toDomain)
                .toList();
    }

    /** @req SWR-026 */
    @Override
    @Transactional(readOnly = true)
    public List<Post> findPublishedByTenantId(TenantId tenantId) {
        return springDataRepo
                .findAllByTenantIdAndStatusOrderByPublishedAtDesc(tenantId.value(), PostStatusJpa.PUBLISHED)
                .stream()
                .map(PostMapper::toDomain)
                .toList();
    }

    /** @req SWR-033 */
    @Override
    @Transactional(readOnly = true)
    public List<Post> findRecentPublishedByTenantId(TenantId tenantId, int limit) {
        return springDataRepo
                .findAllByTenantIdAndStatusOrderByPublishedAtDesc(
                        tenantId.value(), PostStatusJpa.PUBLISHED, PageRequest.of(0, limit))
                .stream()
                .map(PostMapper::toDomain)
                .toList();
    }

    /** @req SWR-026 */
    @Override
    @Transactional(readOnly = true)
    public Optional<Post> findBySlugAndTenantId(Slug slug, TenantId tenantId) {
        return springDataRepo
                .findBySlugAndTenantId(slug.value(), tenantId.value())
                .map(PostMapper::toDomain);
    }

    @Override
    public void deleteByIdAndTenantId(PostId id, TenantId tenantId) {
        springDataRepo.deleteByIdAndTenantId(id.value(), tenantId.value());
    }

    /** @req SWR-038 */
    @Override
    @Transactional(readOnly = true)
    public List<Post> searchPublished(String query, TenantId tenantId) {
        return springDataRepo.searchPublished(query, tenantId.value()).stream()
                .map(PostMapper::toDomain)
                .toList();
    }

    /** @req SWR-039 */
    @Override
    @Transactional(readOnly = true)
    public Optional<Post> findPreviousPublished(TenantId tenantId, Instant publishedAt) {
        return springDataRepo
                .findFirstByTenantIdAndStatusAndPublishedAtBeforeOrderByPublishedAtDesc(
                        tenantId.value(), PostStatusJpa.PUBLISHED, publishedAt)
                .map(PostMapper::toDomain);
    }

    /** @req SWR-039 */
    @Override
    @Transactional(readOnly = true)
    public Optional<Post> findNextPublished(TenantId tenantId, Instant publishedAt) {
        return springDataRepo
                .findFirstByTenantIdAndStatusAndPublishedAtAfterOrderByPublishedAtAsc(
                        tenantId.value(), PostStatusJpa.PUBLISHED, publishedAt)
                .map(PostMapper::toDomain);
    }
}
