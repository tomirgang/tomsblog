package de.tomsblog.blogcontent.application.port.outbound;

import de.tomsblog.blogcontent.domain.model.Post;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.Optional;

public interface PostRepository {

    Post save(Post post);

    Optional<Post> findByIdAndTenantId(PostId id, TenantId tenantId);

    List<Post> findAllByTenantId(TenantId tenantId);

    void deleteByIdAndTenantId(PostId id, TenantId tenantId);
}
