package de.tomsblog.blogcontent.adapter.inbound.web;

import de.tomsblog.blogcontent.application.port.inbound.CreatePostCommand;
import de.tomsblog.blogcontent.application.port.inbound.CreateTagCommand;
import de.tomsblog.blogcontent.application.port.inbound.PostUseCase;
import de.tomsblog.blogcontent.application.port.inbound.TagUseCase;
import de.tomsblog.blogcontent.application.port.inbound.UpdatePostCommand;
import de.tomsblog.blogcontent.application.port.outbound.MarkdownRenderer;
import de.tomsblog.blogcontent.domain.model.ContentType;
import de.tomsblog.blogcontent.domain.model.Post;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.Slug;
import de.tomsblog.blogcontent.domain.model.Tag;
import de.tomsblog.blogcontent.domain.model.TagId;
import de.tomsblog.shared.domain.AuthorId;
import de.tomsblog.shared.tenant.TenantId;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Web adapter serving Thymeleaf views for the public blog UI.
 *
 * @req SWR-025
 * @req SWR-026
 * @req SWR-027
 * @req SWR-033
 * @req SWR-035
 * @req SWR-036
 * @req SWR-037
 * @req SWR-038
 * @req SWR-039
 * @req SWR-040
 * @req SWR-042
 * @req SWR-076
 * @req SWR-077
 * @req SWR-085
 * @req SWR-086
 */
@Controller
public class BlogViewController {

    private static final Pattern FIRST_PARAGRAPH_PATTERN = Pattern.compile("<p[^>]*>(.*?)</p>", Pattern.DOTALL);
    private static final int LANDING_PAGE_LIMIT = 3;

    private final PostUseCase postUseCase;
    private final TagUseCase tagUseCase;
    private final MarkdownRenderer markdownRenderer;

    public BlogViewController(PostUseCase postUseCase, TagUseCase tagUseCase, MarkdownRenderer markdownRenderer) {
        this.postUseCase = postUseCase;
        this.tagUseCase = tagUseCase;
        this.markdownRenderer = markdownRenderer;
    }

    /** @req SWR-033 @req SWR-042 */
    @GetMapping("/")
    public String index(@RequestHeader("X-Tenant-Id") UUID tenantId, Model model, Principal principal) {
        TenantId tenant = new TenantId(tenantId);
        List<Post> posts;
        if (principal != null) {
            posts = postUseCase.listPosts(tenant);
            if (posts.size() > LANDING_PAGE_LIMIT) {
                posts = posts.subList(0, LANDING_PAGE_LIMIT);
            }
        } else {
            posts = postUseCase.listRecentPublishedPosts(tenant, LANDING_PAGE_LIMIT);
        }
        List<Post> featuredPosts = postUseCase.listFeaturedPosts(tenant, LocalDate.now());
        model.addAttribute("posts", posts);
        model.addAttribute("featuredPosts", featuredPosts);
        model.addAttribute("excerpts", buildExcerpts(posts));
        model.addAttribute("featuredExcerpts", buildExcerpts(featuredPosts));
        model.addAttribute("authenticated", principal != null);
        model.addAttribute("landingPage", true);
        model.addAttribute("postTags", buildPostTagMap(posts, tenant));
        model.addAttribute("featuredPostTags", buildPostTagMap(featuredPosts, tenant));
        return "posts/list";
    }

    /** @req SWR-026 @req SWR-028 @req SWR-038 */
    @GetMapping("/posts")
    public String listPosts(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(name = "q", required = false) String query,
            Model model,
            Principal principal) {
        TenantId tenant = new TenantId(tenantId);
        List<Post> posts;
        if (query != null && !query.isBlank()) {
            posts = postUseCase.searchPublishedPosts(query, tenant);
            model.addAttribute("searchQuery", query);
        } else if (principal != null) {
            posts = postUseCase.listPosts(tenant);
        } else {
            posts = postUseCase.listPublishedPosts(tenant);
        }
        model.addAttribute("posts", posts);
        model.addAttribute("excerpts", buildExcerpts(posts));
        model.addAttribute("authenticated", principal != null);
        model.addAttribute("landingPage", false);
        model.addAttribute("postTags", buildPostTagMap(posts, tenant));
        return "posts/list";
    }

    /** @req SWR-026 @req SWR-035 @req SWR-039 @req SWR-040 */
    @GetMapping("/posts/{slug}")
    public String showPost(@PathVariable String slug, @RequestHeader("X-Tenant-Id") UUID tenantId, Model model) {
        Slug postSlug = new Slug(slug);
        TenantId tenant = new TenantId(tenantId);
        Post post = postUseCase.getPublishedPostBySlug(postSlug, tenant);
        model.addAttribute("post", post);
        model.addAttribute("renderedContent", renderContent(post));
        model.addAttribute("tags", resolvePostTags(post, tenant));

        Optional<Post> previousPost = postUseCase.findPreviousPublishedPost(postSlug, tenant);
        Optional<Post> nextPost = postUseCase.findNextPublishedPost(postSlug, tenant);
        previousPost.ifPresent(p -> {
            model.addAttribute("previousPost", p);
            model.addAttribute("previousPostExcerpt", extractFirstParagraph(renderContent(p)));
        });
        nextPost.ifPresent(p -> {
            model.addAttribute("nextPost", p);
            model.addAttribute("nextPostExcerpt", extractFirstParagraph(renderContent(p)));
        });

        addSeriesNavigation(post, previousPost.orElse(null), nextPost.orElse(null), tenant, model);

        return "posts/show";
    }

    /** @req SWR-027 @req SWR-085 */
    @GetMapping("/posts/new")
    public String newPostForm(@RequestHeader("X-Tenant-Id") UUID tenantId, Model model) {
        model.addAttribute("postForm", new PostFormData());
        model.addAttribute("editMode", false);
        model.addAttribute("availablePosts", postUseCase.listPosts(new TenantId(tenantId)));
        model.addAttribute("availableTags", tagUseCase.listTags(new TenantId(tenantId)));
        return "posts/form";
    }

    /** @req SWR-076 */
    @GetMapping("/posts/{id}/preview")
    public String previewPost(@PathVariable UUID id, @RequestHeader("X-Tenant-Id") UUID tenantId, Model model) {
        TenantId tenant = new TenantId(tenantId);
        Post post = postUseCase.getPost(new PostId(id), tenant);
        model.addAttribute("post", post);
        model.addAttribute("renderedContent", renderContent(post));
        model.addAttribute("preview", true);
        model.addAttribute("tags", resolvePostTags(post, tenant));
        return "posts/show";
    }

    /** @req SWR-027 @req SWR-085 */
    @PostMapping("/posts")
    public String createPost(
            @Valid @ModelAttribute("postForm") PostFormData form,
            BindingResult bindingResult,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-Author-Id") UUID authorId,
            Model model) {
        TenantId tenant = new TenantId(tenantId);
        if (bindingResult.hasErrors()) {
            model.addAttribute("editMode", false);
            model.addAttribute("availablePosts", postUseCase.listPosts(tenant));
            model.addAttribute("availableTags", tagUseCase.listTags(tenant));
            return "posts/form";
        }
        CreatePostCommand command = new CreatePostCommand(
                tenant,
                new AuthorId(authorId),
                form.getTitle(),
                form.getContent(),
                form.getContentType(),
                form.getLocale(),
                form.getSocialMediaTitle(),
                form.getSocialMediaSummary(),
                parseUuid(form.getSeriesPreviousPostId()),
                parseUuid(form.getSeriesNextPostId()),
                parseLocalDate(form.getFeaturedFrom()),
                parseLocalDate(form.getFeaturedUntil()));
        Post post = postUseCase.createPost(command);
        applyTags(post, form, tenant);
        return "redirect:/posts";
    }

    /** @req SWR-027 @req SWR-085 */
    @GetMapping("/posts/{id}/edit")
    public String editPostForm(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Author-Id", required = false) UUID authorId,
            Model model) {
        TenantId tenant = new TenantId(tenantId);
        Post post = postUseCase.getPost(new PostId(id), tenant);
        verifyOwnership(post, authorId);
        List<String> existingTagIds =
                post.getTags().stream().map(tagId -> tagId.value().toString()).toList();
        PostFormData form = new PostFormData(
                post.getTitle(),
                post.getContent(),
                post.getContentType().name(),
                post.getLocale().languageTag(),
                post.getSocialMediaTitle(),
                post.getSocialMediaSummary(),
                post.getSeriesPreviousPostId() != null
                        ? post.getSeriesPreviousPostId().value().toString()
                        : null,
                post.getSeriesNextPostId() != null
                        ? post.getSeriesNextPostId().value().toString()
                        : null,
                post.getFeaturedFrom() != null ? post.getFeaturedFrom().toString() : null,
                post.getFeaturedUntil() != null ? post.getFeaturedUntil().toString() : null,
                existingTagIds);
        model.addAttribute("postForm", form);
        model.addAttribute("editMode", true);
        model.addAttribute("postId", id);
        model.addAttribute("availablePosts", postUseCase.listPosts(tenant));
        model.addAttribute("availableTags", tagUseCase.listTags(tenant));
        return "posts/form";
    }

    /** @req SWR-027 @req SWR-085 */
    @PostMapping("/posts/{id}")
    public String updatePost(
            @PathVariable UUID id,
            @Valid @ModelAttribute("postForm") PostFormData form,
            BindingResult bindingResult,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Author-Id", required = false) UUID authorId,
            Model model) {
        TenantId tenant = new TenantId(tenantId);
        {
            Post existing = postUseCase.getPost(new PostId(id), tenant);
            verifyOwnership(existing, authorId);
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("editMode", true);
            model.addAttribute("postId", id);
            model.addAttribute("availablePosts", postUseCase.listPosts(tenant));
            model.addAttribute("availableTags", tagUseCase.listTags(tenant));
            return "posts/form";
        }
        UpdatePostCommand command = new UpdatePostCommand(
                new PostId(id),
                tenant,
                form.getTitle(),
                form.getContent(),
                form.getContentType(),
                form.getSocialMediaTitle(),
                form.getSocialMediaSummary(),
                parseUuid(form.getSeriesPreviousPostId()),
                parseUuid(form.getSeriesNextPostId()),
                parseLocalDate(form.getFeaturedFrom()),
                parseLocalDate(form.getFeaturedUntil()));
        Post post = postUseCase.updatePost(command);
        applyTags(post, form, tenant);
        return "redirect:/posts";
    }

    /** @req SWR-002 */
    @PostMapping("/posts/{id}/publish")
    public String publishPost(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Author-Id", required = false) UUID authorId) {
        Post post = postUseCase.getPost(new PostId(id), new TenantId(tenantId));
        verifyOwnership(post, authorId);
        postUseCase.publishPost(new PostId(id), new TenantId(tenantId));
        return "redirect:/posts";
    }

    private static void verifyOwnership(Post post, UUID authorId) {
        if (authorId == null) {
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.getAuthorities().stream()
                        .anyMatch(a ->
                                "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPERADMIN".equals(a.getAuthority()))) {
            return;
        }
        if (!post.getAuthorId().value().equals(authorId)) {
            throw new AccessDeniedException("Not the post author");
        }
    }

    /**
     * Builds rendered HTML excerpts from the first paragraph of each post.
     *
     * @req SWR-036
     * @req SWR-037
     */
    private Map<UUID, String> buildExcerpts(List<Post> posts) {
        Map<UUID, String> excerpts = new LinkedHashMap<>();
        for (Post post : posts) {
            String html = renderContent(post);
            String excerpt = extractFirstParagraph(html);
            excerpts.put(post.getId().value(), excerpt);
        }
        return excerpts;
    }

    /** @req SWR-035 */
    private String renderContent(Post post) {
        if (post.getContentType() == ContentType.MARKDOWN) {
            return markdownRenderer.renderToHtml(post.getContent());
        }
        return post.getContent();
    }

    private static String extractFirstParagraph(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Matcher matcher = FIRST_PARAGRAPH_PATTERN.matcher(html);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return html;
    }

    /**
     * Adds series navigation to the model if the post has series links that differ from chronological navigation.
     *
     * @req SWR-040
     */
    private void addSeriesNavigation(Post post, Post chronoPrev, Post chronoNext, TenantId tenantId, Model model) {
        if (post.getSeriesPreviousPostId() != null) {
            boolean sameAsChronoPrev = chronoPrev != null && chronoPrev.getId().equals(post.getSeriesPreviousPostId());
            if (!sameAsChronoPrev) {
                postUseCase
                        .getPostIfPublished(post.getSeriesPreviousPostId(), tenantId)
                        .ifPresent(p -> model.addAttribute("seriesPreviousPost", p));
            }
        }
        if (post.getSeriesNextPostId() != null) {
            boolean sameAsChronoNext = chronoNext != null && chronoNext.getId().equals(post.getSeriesNextPostId());
            if (!sameAsChronoNext) {
                postUseCase
                        .getPostIfPublished(post.getSeriesNextPostId(), tenantId)
                        .ifPresent(p -> model.addAttribute("seriesNextPost", p));
            }
        }
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }

    private static LocalDate parseLocalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }

    /**
     * Resolves TagIds of a post to full Tag objects.
     *
     * @req SWR-086
     */
    private List<Tag> resolvePostTags(Post post, TenantId tenantId) {
        if (post.getTags().isEmpty()) {
            return List.of();
        }
        Map<UUID, Tag> tagIndex = tagUseCase.listTags(tenantId).stream()
                .collect(Collectors.toMap(t -> t.getId().value(), t -> t));
        return post.getTags().stream()
                .map(tagId -> tagIndex.get(tagId.value()))
                .filter(t -> t != null)
                .toList();
    }

    /**
     * Builds a map from post UUID to its resolved Tag list for use in list views.
     *
     * @req SWR-086
     */
    private Map<UUID, List<Tag>> buildPostTagMap(List<Post> posts, TenantId tenantId) {
        boolean anyPostHasTags = posts.stream().anyMatch(p -> !p.getTags().isEmpty());
        if (!anyPostHasTags) {
            return Map.of();
        }
        Map<UUID, Tag> tagIndex = tagUseCase.listTags(tenantId).stream()
                .collect(Collectors.toMap(t -> t.getId().value(), t -> t));
        Map<UUID, List<Tag>> result = new LinkedHashMap<>();
        for (Post post : posts) {
            List<Tag> resolved = post.getTags().stream()
                    .map(tagId -> tagIndex.get(tagId.value()))
                    .filter(t -> t != null)
                    .toList();
            if (!resolved.isEmpty()) {
                result.put(post.getId().value(), resolved);
            }
        }
        return result;
    }

    /**
     * Applies tag selections from form to a post. Creates new tags if requested.
     *
     * @req SWR-085
     */
    private void applyTags(Post post, PostFormData form, TenantId tenantId) {
        Set<TagId> selectedTags = form.getTagIds().stream()
                .filter(s -> s != null && !s.isBlank())
                .map(UUID::fromString)
                .map(TagId::new)
                .collect(Collectors.toSet());

        // Create new tag if requested
        if (form.getNewTagName() != null && !form.getNewTagName().isBlank()) {
            Tag newTag = tagUseCase.createTag(new CreateTagCommand(tenantId, form.getNewTagName()));
            selectedTags.add(newTag.getId());
        }

        postUseCase.syncPostTags(post.getId(), tenantId, selectedTags);
    }
}
