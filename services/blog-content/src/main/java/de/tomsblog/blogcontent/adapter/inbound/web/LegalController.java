package de.tomsblog.blogcontent.adapter.inbound.web;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.TenantSettingsDto;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web adapter serving Impressum and Privacy Policy pages.
 *
 * @req SWR-054
 * @req SWR-055
 */
@Controller
public class LegalController {

    private static final Logger LOG = LoggerFactory.getLogger(LegalController.class);

    private final UserManagementClient userManagementClient;

    public LegalController(UserManagementClient userManagementClient) {
        this.userManagementClient = userManagementClient;
    }

    @GetMapping("/impressum")
    public String impressum(HttpServletRequest request, HttpSession session, Model model) {
        TenantSettingsDto settings = resolveSettings(request, session);
        if (settings != null
                && settings.impressumContent() != null
                && !settings.impressumContent().isBlank()) {
            model.addAttribute("customContent", settings.impressumContent());
        }
        return "legal/impressum";
    }

    @GetMapping("/privacy")
    public String privacy(HttpServletRequest request, HttpSession session, Model model) {
        TenantSettingsDto settings = resolveSettings(request, session);
        if (settings != null
                && settings.privacyPolicyContent() != null
                && !settings.privacyPolicyContent().isBlank()) {
            model.addAttribute("customContent", settings.privacyPolicyContent());
        }
        return "legal/privacy";
    }

    private TenantSettingsDto resolveSettings(HttpServletRequest request, HttpSession session) {
        UUID tenantId = resolveActiveTenantId(request, session);
        try {
            return userManagementClient.getTenantSettings(tenantId);
        } catch (Exception e) {
            LOG.debug("Failed to load tenant settings for '{}'.", tenantId, e);
            return null;
        }
    }

    private UUID resolveActiveTenantId(HttpServletRequest request, HttpSession session) {
        Object sessionTenant = session.getAttribute("activeTenantId");
        if (sessionTenant != null) {
            try {
                return UUID.fromString(sessionTenant.toString());
            } catch (IllegalArgumentException e) {
                // Ignore invalid session value
            }
        }
        String header = request.getHeader("X-Tenant-Id");
        if (header != null) {
            try {
                return UUID.fromString(header);
            } catch (IllegalArgumentException e) {
                // Ignore invalid header
            }
        }
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }
}
