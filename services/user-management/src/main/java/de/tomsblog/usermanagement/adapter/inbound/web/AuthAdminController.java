package de.tomsblog.usermanagement.adapter.inbound.web;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.application.port.inbound.UserProfileUseCase;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.Role;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import de.tomsblog.usermanagement.domain.model.UserProfile;
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
 * Controller for the admin UI pages (ADR-0032).
 *
 * <p>Accesses the local {@link UserProfileUseCase} and {@link TenantSettingsUseCase} directly.
 *
 * @req SWR-051
 * @req SWR-052
 * @req SWR-053
 * @req SWR-069
 * @req SWR-070
 * @req SWR-071
 */
@Controller
@RequestMapping("/auth/admin")
public class AuthAdminController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthAdminController.class);

    private final UserProfileUseCase userProfileUseCase;
    private final TenantSettingsUseCase tenantSettingsUseCase;

    public AuthAdminController(UserProfileUseCase userProfileUseCase, TenantSettingsUseCase tenantSettingsUseCase) {
        this.userProfileUseCase = userProfileUseCase;
        this.tenantSettingsUseCase = tenantSettingsUseCase;
    }

    @GetMapping("/users")
    public String listUsers(@RequestHeader("X-Tenant-Id") UUID tenantId, HttpSession session, Model model) {
        UUID activeTenant = resolveActiveTenant(tenantId, session);
        try {
            List<UserProfile> users = userProfileUseCase.listByTenantId(new TenantId(activeTenant));
            model.addAttribute("users", users);
        } catch (Exception e) {
            LOG.warn("Failed to load users for tenant '{}'.", activeTenant, e);
            model.addAttribute("users", List.of());
        }
        model.addAttribute("tenantId", activeTenant);
        return "admin/users";
    }

    @PostMapping("/users/{identifier}/approve")
    public String approveUser(@PathVariable String identifier) {
        userProfileUseCase.approveUser(identifier);
        return "redirect:/auth/admin/users";
    }

    @PostMapping("/users/{identifier}/reject")
    public String rejectUser(@PathVariable String identifier) {
        userProfileUseCase.rejectUser(identifier);
        return "redirect:/auth/admin/users";
    }

    @PostMapping("/users/{identifier}/role")
    public String changeRole(
            @PathVariable String identifier,
            @RequestParam String role,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            HttpSession session) {
        UUID activeTenant = resolveActiveTenant(tenantId, session);
        userProfileUseCase.addTenantMembership(identifier, new TenantId(activeTenant), Role.valueOf(role));
        return "redirect:/auth/admin/users";
    }

    @GetMapping("/settings")
    public String showSettings(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            HttpSession session,
            @RequestParam(defaultValue = "general") String tab,
            Model model) {
        UUID activeTenant = resolveActiveTenant(tenantId, session);
        try {
            TenantSettings settings = tenantSettingsUseCase.getSettings(new TenantId(activeTenant));
            model.addAttribute("settings", settings);
        } catch (Exception e) {
            LOG.warn("Failed to load tenant settings for '{}'.", activeTenant, e);
        }
        model.addAttribute("activeTab", tab);
        return "admin/settings";
    }

    @PostMapping("/settings/general")
    public String updateGeneralSettings(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            HttpSession session,
            @RequestParam String loginMode,
            @RequestParam(defaultValue = "false") boolean autoApproveOidc,
            @RequestParam(defaultValue = "") String autoApproveEmailDomains,
            @RequestParam String displayName,
            @RequestParam(required = false) String tagline) {
        UUID activeTenant = resolveActiveTenant(tenantId, session);
        Set<String> domains = parseDomains(autoApproveEmailDomains);
        tenantSettingsUseCase.updateGeneralSettings(
                new TenantId(activeTenant),
                displayName,
                tagline,
                LoginMode.valueOf(loginMode),
                autoApproveOidc,
                domains);
        return "redirect:/auth/admin/settings?tab=general";
    }

    @PostMapping("/settings/oidc")
    public String updateOidcSettings(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            HttpSession session,
            @RequestParam(required = false) String oidcIssuerUrl,
            @RequestParam(required = false) String oidcClientId,
            @RequestParam(required = false) String oidcClientSecret) {
        UUID activeTenant = resolveActiveTenant(tenantId, session);
        tenantSettingsUseCase.updateOidcSettings(
                new TenantId(activeTenant), oidcIssuerUrl, oidcClientId, oidcClientSecret);
        return "redirect:/auth/admin/settings?tab=oidc";
    }

    @PostMapping("/settings/legal")
    public String updateLegalSettings(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            HttpSession session,
            @RequestParam(required = false) String impressumContent,
            @RequestParam(required = false) String privacyPolicyContent) {
        UUID activeTenant = resolveActiveTenant(tenantId, session);
        tenantSettingsUseCase.updateLegalSettings(new TenantId(activeTenant), impressumContent, privacyPolicyContent);
        return "redirect:/auth/admin/settings?tab=legal";
    }

    @PostMapping("/switch-tenant")
    public String switchTenant(@RequestParam UUID tenantId, HttpSession session, HttpServletRequest request) {
        session.setAttribute("activeTenantId", tenantId.toString());
        String referer = request.getHeader("Referer");
        if (referer != null && isSafeRedirect(referer, request)) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }

    private static boolean isSafeRedirect(String url, HttpServletRequest request) {
        try {
            var uri = java.net.URI.create(url);
            return !uri.isAbsolute() || request.getServerName().equals(uri.getHost());
        } catch (Exception e) {
            return false;
        }
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

    private Set<String> parseDomains(String autoApproveEmailDomains) {
        Set<String> domains = new LinkedHashSet<>();
        if (!autoApproveEmailDomains.isBlank()) {
            Arrays.stream(autoApproveEmailDomains.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(domains::add);
        }
        return domains;
    }
}
