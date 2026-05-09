package de.tomsblog.tenantmanagement.adapter.inbound.web;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.tenantmanagement.application.port.inbound.TenantManagementUseCase;
import de.tomsblog.tenantmanagement.domain.model.Tenant;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for the tenant admin UI (ADR-0031).
 *
 * @req SWR-073
 */
@Controller
@RequestMapping("/tenant/admin")
public class TenantAdminController {

    private static final Logger LOG = LoggerFactory.getLogger(TenantAdminController.class);

    private final TenantManagementUseCase tenantManagementUseCase;

    public TenantAdminController(TenantManagementUseCase tenantManagementUseCase) {
        this.tenantManagementUseCase = tenantManagementUseCase;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "tenant-admin-login";
    }

    @GetMapping("/tenants")
    public String listTenants(Model model) {
        try {
            List<Tenant> tenants = tenantManagementUseCase.listAllTenants();
            model.addAttribute("tenants", tenants);
        } catch (Exception e) {
            LOG.warn("Failed to load tenants.", e);
            model.addAttribute("tenants", List.of());
        }
        return "admin/tenants";
    }

    @PostMapping("/tenants/new")
    public String createTenant(@RequestParam String slug, @RequestParam String displayName) {
        tenantManagementUseCase.createTenant(slug, displayName);
        return "redirect:/tenant/admin/tenants";
    }

    @GetMapping("/settings")
    public String showSettings(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            HttpSession session,
            @RequestParam(defaultValue = "general") String tab,
            Model model) {
        UUID activeTenant = resolveActiveTenant(tenantId, session);
        try {
            Tenant tenant = tenantManagementUseCase.getTenant(new TenantId(activeTenant));
            model.addAttribute("tenant", tenant);
        } catch (Exception e) {
            LOG.warn("Failed to load tenant settings for '{}'.", activeTenant, e);
        }
        model.addAttribute("activeTab", tab);
        return "tenant-admin/settings";
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
        tenantManagementUseCase.updateGeneralSettings(
                new TenantId(activeTenant), displayName, tagline, loginMode, autoApproveOidc, domains);
        return "redirect:/tenant/admin/settings?tab=general";
    }

    @PostMapping("/settings/oidc")
    public String updateOidcSettings(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            HttpSession session,
            @RequestParam(required = false) String oidcIssuerUrl,
            @RequestParam(required = false) String oidcClientId,
            @RequestParam(required = false) String oidcClientSecret) {
        UUID activeTenant = resolveActiveTenant(tenantId, session);
        tenantManagementUseCase.updateOidcSettings(
                new TenantId(activeTenant), oidcIssuerUrl, oidcClientId, oidcClientSecret);
        return "redirect:/tenant/admin/settings?tab=oidc";
    }

    @PostMapping("/settings/legal")
    public String updateLegalSettings(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            HttpSession session,
            @RequestParam(required = false) String impressumContent,
            @RequestParam(required = false) String privacyPolicyContent) {
        UUID activeTenant = resolveActiveTenant(tenantId, session);
        tenantManagementUseCase.updateLegalSettings(new TenantId(activeTenant), impressumContent, privacyPolicyContent);
        return "redirect:/tenant/admin/settings?tab=legal";
    }

    @PostMapping("/switch-tenant")
    public String switchTenant(@RequestParam UUID tenantId, HttpSession session, HttpServletRequest request) {
        session.setAttribute("activeTenantId", tenantId.toString());
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/tenant/admin/tenants");
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
