package de.tomsblog.blogcontent.application.port.inbound;

import de.tomsblog.blogcontent.domain.model.Post;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.Source;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;

/**
 * Inbound port for post management use cases.
 *
 * @req SWR-001
 * @req SWR-002
 * @req SWR-012
 */
public interface PostUseCase {

    /** @req SWR-001 */
    Post createPost(CreatePostCommand command);

    /** @req SWR-001 */
    Post updatePost(UpdatePostCommand command);

    /** @req SWR-002 */
    void publishPost(PostId postId, TenantId tenantId);

    /** @req SWR-001 */
    void deletePost(PostId postId, TenantId tenantId);

    /** @req SWR-001 */
    Post getPost(PostId postId, TenantId tenantId);

    /** @req SWR-001 */
    List<Post> listPosts(TenantId tenantId);

    /**
     * @req SWR-012
     */
    Post addSource(AddSourceCommand command);

    /**
     * @req SWR-012
     */
    Post removeSource(RemoveSourceCommand command);

    /**
     * @req SWR-012
     */
    List<Source> listSources(PostId postId, TenantId tenantId);
}
