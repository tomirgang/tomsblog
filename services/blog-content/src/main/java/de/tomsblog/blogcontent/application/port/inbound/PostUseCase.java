package de.tomsblog.blogcontent.application.port.inbound;

import de.tomsblog.blogcontent.domain.model.Post;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;

public interface PostUseCase {

    Post createPost(CreatePostCommand command);

    Post updatePost(UpdatePostCommand command);

    void publishPost(PostId postId, TenantId tenantId);

    void deletePost(PostId postId, TenantId tenantId);

    Post getPost(PostId postId, TenantId tenantId);

    List<Post> listPosts(TenantId tenantId);
}
