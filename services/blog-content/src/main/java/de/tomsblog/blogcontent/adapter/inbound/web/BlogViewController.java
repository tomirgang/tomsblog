package de.tomsblog.blogcontent.adapter.inbound.web;

import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.domain.model.Post;
import de.tomsblog.blogcontent.domain.model.Slug;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Web adapter serving Thymeleaf views for the public blog UI.
 *
 * @req SWR-025
 * @req SWR-026
 */
@Controller
public class BlogViewController {

    private final PostUseCase postUseCase;

    public BlogViewController(PostUseCase postUseCase) {
        this.postUseCase = postUseCase;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    /** @req SWR-026 */
    @GetMapping("/posts")
    public String listPosts(@RequestHeader("X-Tenant-Id") UUID tenantId, Model model) {
        List<Post> posts = postUseCase.listPublishedPosts(new TenantId(tenantId));
        model.addAttribute("posts", posts);
        return "posts/list";
    }

    /** @req SWR-026 */
    @GetMapping("/posts/{slug}")
    public String showPost(@PathVariable String slug, @RequestHeader("X-Tenant-Id") UUID tenantId, Model model) {
        Post post = postUseCase.getPublishedPostBySlug(new Slug(slug), new TenantId(tenantId));
        model.addAttribute("post", post);
        return "posts/show";
    }
}
