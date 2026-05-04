package de.tomsblog.blogcontent.adapter.inbound.web;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.TenantSettingsDto;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserProfileDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for the admin UI pages (user management and tenant settings).
 *
 * @req SWR-051
 * @req SWR-052
 * @req SWR-053
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger LOG = LoggerFactory.getLogger(AdminController.class);

    private final UserManagementClient userManagementClient;

    public AdminController(UserManagementClient userManagementClient) {
        this.userManagementClient = userManagementClient;
    }

    @GetMapping("/users")
    public String listUsers(@RequestHeader("X-Tenant-Id") UUID tenantId, HttpSession session, Model model) {
        UUID activeTenant = resolveActiveTenant(tenantId, session);
        try {
            List<UserProfileDto> users = userManagementClient.listUsersByTenant(activeTenant);
            model.addAttribute("users", users);
        } catch (Exception e) {
            LOG.warn("Failed to load users for tenant '{}'.", activeTenant, e);
            model.addAttribute("users", List.of());
        }
        return "admin/users";
    }

    @PostMapping("/users/{identifier}/approve")
    public String approveUser(@PathVariable String identifier) {
        userManagementClient.approveUser(identifier);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{identifier}/reject")
    public String rejectUser(@PathVariable String identifier) {
        userManagementClient.rejectUser(identifier);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{identifier}/role")
    public String changeRole(
            @PathVariable String identifier,
            @RequestParam String role,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            HttpSession session) {
        UUID activeTenant = resolveActiveTenant(tenantId, session);
        userManagementClient.changeUserRole(identifier, activeTenant, role);
        return "redirect:/admin/users";
    }

    @GetMapping("/settings")
    public String showSettings(@RequestHeader("X-Tenant-Id") UUID tenantId, HttpSession session, Model model) {
        UUID activeTenant = resolveActiveTenant(tenantId, session);
        try {
            TenantSettingsDto settings = userManagementClient.getTenantSettings(activeTenant);
            model.addAttribute("settings", settings);
        } catch (Exception e) {
            LOG.warn("Failed to load tenant settings for '{}'.", activeTenant, e);
            model.addAttribute(
                    "settings",
                    new TenantSettingsDto(activeTenant, "BOTH", false, Set.of(), "Toms Blog", null, null, null));
        }
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String updateSettings(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            HttpSession session,
            @RequestParam String loginMode,
            @RequestParam(defaultValue = "false") boolean autoApproveOidc,
            @RequestParam(defaultValue = "") String autoApproveEmailDomains,
            @RequestParam String displayName,
            @RequestParam(required = false) String tagline,
            @RequestParam(required = false) String impressumContent,
            @RequestParam(required = false) String privacyPolicyContent) {
        UUID activeTenant = resolveActiveTenant(tenantId, session);
        Set<String> domains = new LinkedHashSet<>();
        if (!autoApproveEmailDomains.isBlank()) {
            Arrays.stream(autoApproveEmailDomains.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(domains::add);
        }
        userManagementClient.updateTenantSettings(
                activeTenant,
                loginMode,
                autoApproveOidc,
                domains,
                displayName,
                tagline,
                impressumContent,
                privacyPolicyContent);
        return "redirect:/admin/settings";
    }

    @PostMapping("/switch-tenant")
    public String switchTenant(@RequestParam UUID tenantId, HttpSession session, HttpServletRequest request) {
        session.setAttribute("activeTenantId", tenantId.toString());
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    private UUID resolveActiveTenant(UUID headerTenantId, HttpSession session) {
        Object sessionTenant = session.getAttribute("activeTenantId");
        if (sessionTenant != null) {
            try {
                return UUID.fromString(sessionTenant.toString());
            } catch (IllegalArgumentException e) {
                // Ignore invalid session value
            }
        }
        return headerTenantId;
    }
}
