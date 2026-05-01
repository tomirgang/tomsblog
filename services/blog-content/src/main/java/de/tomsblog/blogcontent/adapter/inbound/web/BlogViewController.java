package de.tomsblog.blogcontent.adapter.inbound.web;

import de.tomsblog.blogcontent.application.port.inbound.CreatePostCommand;
import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.application.port.inbound.UpdatePostCommand;
import de.tomsblog.blogcontent.domain.model.Post;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.Slug;
import de.tomsblog.shared.domain.AuthorId;
import de.tomsblog.shared.tenant.TenantId;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Web adapter serving Thymeleaf views for the public blog UI.
 *
 * @req SWR-025
 * @req SWR-026
 * @req SWR-027
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
        List<Post> posts = postUseCase.listPosts(new TenantId(tenantId));
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

    /** @req SWR-027 */
    @GetMapping("/posts/new")
    public String newPostForm(Model model) {
        model.addAttribute("postForm", new PostFormData());
        model.addAttribute("editMode", false);
        return "posts/form";
    }

    /** @req SWR-027 */
    @PostMapping("/posts")
    public String createPost(
            @Valid @ModelAttribute("postForm") PostFormData form,
            BindingResult bindingResult,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-Author-Id") UUID authorId,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editMode", false);
            return "posts/form";
        }
        CreatePostCommand command = new CreatePostCommand(
                new TenantId(tenantId),
                new AuthorId(authorId),
                form.getTitle(),
                form.getContent(),
                form.getLocale(),
                form.getSocialMediaTitle(),
                form.getSocialMediaSummary());
        postUseCase.createPost(command);
        return "redirect:/posts";
    }

    /** @req SWR-027 */
    @GetMapping("/posts/{id}/edit")
    public String editPostForm(@PathVariable UUID id, @RequestHeader("X-Tenant-Id") UUID tenantId, Model model) {
        Post post = postUseCase.getPost(new PostId(id), new TenantId(tenantId));
        PostFormData form = new PostFormData(
                post.getTitle(),
                post.getContent(),
                post.getLocale().languageTag(),
                post.getSocialMediaTitle(),
                post.getSocialMediaSummary());
        model.addAttribute("postForm", form);
        model.addAttribute("editMode", true);
        model.addAttribute("postId", id);
        return "posts/form";
    }

    /** @req SWR-027 */
    @PostMapping("/posts/{id}")
    public String updatePost(
            @PathVariable UUID id,
            @Valid @ModelAttribute("postForm") PostFormData form,
            BindingResult bindingResult,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editMode", true);
            model.addAttribute("postId", id);
            return "posts/form";
        }
        UpdatePostCommand command = new UpdatePostCommand(
                new PostId(id),
                new TenantId(tenantId),
                form.getTitle(),
                form.getContent(),
                form.getSocialMediaTitle(),
                form.getSocialMediaSummary());
        postUseCase.updatePost(command);
        return "redirect:/posts";
    }

    /** @req SWR-002 */
    @PostMapping("/posts/{id}/publish")
    public String publishPost(@PathVariable UUID id, @RequestHeader("X-Tenant-Id") UUID tenantId) {
        postUseCase.publishPost(new PostId(id), new TenantId(tenantId));
        return "redirect:/posts";
    }
}
