package de.tomsblog.blogcontent.adapter.inbound.web;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.TenantSettingsDto;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import io.grpc.StatusRuntimeException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for local user registration (SWR-060).
 *
 * <p>Provides a Thymeleaf-based registration form at /register. The form is only available when the
 * tenant's LoginMode is INTERNAL or BOTH (SWR-044).
 *
 * @req SWR-059
 * @req SWR-060
 */
@Controller
public class RegistrationController {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationController.class);
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserManagementClient userManagementClient;

    public RegistrationController(UserManagementClient userManagementClient) {
        this.userManagementClient = userManagementClient;
    }

    @GetMapping("/register")
    public String showRegistrationForm(@RequestHeader("X-Tenant-Id") UUID tenantId, Model model) {
        String loginMode = resolveLoginMode(tenantId);
        if ("OIDC".equals(loginMode)) {
            return "redirect:/login";
        }
        model.addAttribute("loginMode", loginMode);
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam(required = false) String displayName,
            @RequestParam String password,
            @RequestParam String passwordConfirm,
            Model model) {

        String loginMode = resolveLoginMode(tenantId);
        if ("OIDC".equals(loginMode)) {
            return "redirect:/login";
        }
        model.addAttribute("loginMode", loginMode);
        model.addAttribute("username", username);
        model.addAttribute("email", email);
        model.addAttribute("displayName", displayName);

        // Validate password confirmation
        if (!password.equals(passwordConfirm)) {
            model.addAttribute("error", "Die Passwörter stimmen nicht überein.");
            return "register";
        }

        // Validate password length
        if (password.length() < MIN_PASSWORD_LENGTH) {
            model.addAttribute("error", "Das Passwort muss mindestens " + MIN_PASSWORD_LENGTH + " Zeichen lang sein.");
            return "register";
        }

        // Validate required fields
        if (username.isBlank() || email.isBlank()) {
            model.addAttribute("error", "Benutzername und E-Mail sind Pflichtfelder.");
            return "register";
        }

        try {
            userManagementClient.registerUser(username, password, email, displayName, tenantId);
            return "registration-success";
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.ALREADY_EXISTS) {
                model.addAttribute("error", "Benutzername oder E-Mail-Adresse ist bereits vergeben.");
            } else if (e.getStatus().getCode() == io.grpc.Status.Code.INVALID_ARGUMENT) {
                model.addAttribute("error", e.getStatus().getDescription());
            } else {
                LOG.error("Registration failed for user '{}'", username, e);
                model.addAttribute("error", "Die Registrierung ist fehlgeschlagen. Bitte versuchen Sie es erneut.");
            }
            return "register";
        }
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
