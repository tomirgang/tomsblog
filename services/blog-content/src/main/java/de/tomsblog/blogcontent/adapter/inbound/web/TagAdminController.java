package de.tomsblog.blogcontent.adapter.inbound.web;

import de.tomsblog.blogcontent.application.port.inbound.CreateTagCommand;
import de.tomsblog.blogcontent.application.port.inbound.RenameTagCommand;
import de.tomsblog.blogcontent.application.port.inbound.TagUseCase;
import de.tomsblog.blogcontent.domain.model.Tag;
import de.tomsblog.blogcontent.domain.model.TagId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Web adapter for tag management in the admin area.
 *
 * @req SWR-084
 */
@Controller
@RequestMapping("/admin/tags")
public class TagAdminController {

    private final TagUseCase tagUseCase;

    public TagAdminController(TagUseCase tagUseCase) {
        this.tagUseCase = tagUseCase;
    }

    /** @req SWR-084 */
    @GetMapping
    public String listTags(@RequestHeader("X-Tenant-Id") UUID tenantId, Model model) {
        List<Tag> tags = tagUseCase.listTags(new TenantId(tenantId));
        model.addAttribute("tags", tags);
        return "admin/tags";
    }

    /** @req SWR-084 */
    @PostMapping
    public String createTag(@RequestHeader("X-Tenant-Id") UUID tenantId, @RequestParam String name) {
        tagUseCase.createTag(new CreateTagCommand(new TenantId(tenantId), name));
        return "redirect:/admin/tags";
    }

    /** @req SWR-084 */
    @PostMapping("/{id}/rename")
    public String renameTag(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID id, @RequestParam String name) {
        tagUseCase.renameTag(new RenameTagCommand(new TagId(id), new TenantId(tenantId), name));
        return "redirect:/admin/tags";
    }

    /** @req SWR-084 */
    @PostMapping("/{id}/delete")
    public String deleteTag(@RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID id) {
        tagUseCase.deleteTag(new TagId(id), new TenantId(tenantId));
        return "redirect:/admin/tags";
    }
}
