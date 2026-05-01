package de.tomsblog.blogcontent.adapter.outbound.persistence;

import de.tomsblog.blogcontent.application.port.outbound.PostRepository;
import de.tomsblog.blogcontent.domain.model.Post;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA implementation of the post repository port.
 *
 * @req SWR-001
 * @req SWR-003
 * @req SWR-006
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

    @Override
    public void deleteByIdAndTenantId(PostId id, TenantId tenantId) {
        springDataRepo.deleteByIdAndTenantId(id.value(), tenantId.value());
    }
}
