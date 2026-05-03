package de.tomsblog.blogcontent.adapter.inbound.web;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.TenantSettingsDto;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Controller serving the login pages.
 *
 * @req SWR-016
 * @req SWR-028
 * @req SWR-044
 */
@Controller
public class LoginController {

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

    private final UserManagementClient userManagementClient;

    public LoginController(UserManagementClient userManagementClient) {
        this.userManagementClient = userManagementClient;
    }

    @GetMapping("/login")
    public String login(@RequestHeader("X-Tenant-Id") UUID tenantId, Model model) {
        String loginMode = resolveLoginMode(tenantId);
        model.addAttribute("loginMode", loginMode);
        return "login";
    }

    @GetMapping("/admin/login")
    public String adminLogin() {
        return "admin-login";
    }

    private String resolveLoginMode(UUID tenantId) {
        try {
            TenantSettingsDto settings = userManagementClient.getTenantSettings(tenantId);
            return settings != null ? settings.loginMode() : "BOTH";
        } catch (Exception e) {
            LOG.warn("Failed to retrieve tenant settings for tenant '{}'. Defaulting to BOTH.", tenantId, e);
            return "BOTH";
        }
    }
}
