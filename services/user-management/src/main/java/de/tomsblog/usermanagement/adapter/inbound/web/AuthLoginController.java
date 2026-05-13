package de.tomsblog.usermanagement.adapter.inbound.web;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller serving the login pages (ADR-0032).
 *
 * <p>Reads the tenant's LoginMode directly from the local database (no gRPC roundtrip).
 *
 * @req SWR-016
 * @req SWR-028
 * @req SWR-044
 */
@Controller
@RequestMapping("/auth")
public class AuthLoginController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthLoginController.class);

    private final TenantSettingsUseCase tenantSettingsUseCase;

    public AuthLoginController(TenantSettingsUseCase tenantSettingsUseCase) {
        this.tenantSettingsUseCase = tenantSettingsUseCase;
    }

    @GetMapping("/login")
    public String login(@RequestHeader("X-Tenant-Id") UUID tenantId, Model model) {
        String loginMode = resolveLoginMode(tenantId);
        model.addAttribute("loginMode", loginMode);
        model.addAttribute("oidcButtonText", resolveOidcButtonText(tenantId));
        return "login";
    }

    @GetMapping("/admin/login")
    public String adminLogin() {
        return "admin-login";
    }

    private String resolveLoginMode(UUID tenantId) {
        try {
            TenantSettings settings = tenantSettingsUseCase.getSettings(new TenantId(tenantId));
            return settings != null ? settings.getLoginMode().name() : "BOTH";
        } catch (Exception e) {
            LOG.warn("Failed to retrieve tenant settings for tenant '{}'. Defaulting to BOTH.", tenantId, e);
            return "BOTH";
        }
    }

    private String resolveOidcButtonText(UUID tenantId) {
        try {
            TenantSettings settings = tenantSettingsUseCase.getSettings(new TenantId(tenantId));
            if (settings != null
                    && settings.getOidcButtonText() != null
                    && !settings.getOidcButtonText().isBlank()) {
                return settings.getOidcButtonText();
            }
        } catch (Exception e) {
            LOG.warn("Failed to retrieve OIDC button text for tenant '{}'.", tenantId, e);
        }
        return "Mit OIDC anmelden";
    }
}
