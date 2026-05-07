package de.tomsblog.usermanagement.adapter.inbound.web;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.RegisterUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.application.port.inbound.UserProfileUseCase;
import de.tomsblog.usermanagement.application.service.UserAlreadyExistsException;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
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
 * Controller for local user registration (ADR-0032).
 *
 * <p>Calls the local {@link UserProfileUseCase} directly instead of going through gRPC.
 *
 * @req SWR-059
 * @req SWR-060
 */
@Controller
@RequestMapping("/auth")
public class AuthRegistrationController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthRegistrationController.class);
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserProfileUseCase userProfileUseCase;
    private final TenantSettingsUseCase tenantSettingsUseCase;

    public AuthRegistrationController(
            UserProfileUseCase userProfileUseCase, TenantSettingsUseCase tenantSettingsUseCase) {
        this.userProfileUseCase = userProfileUseCase;
        this.tenantSettingsUseCase = tenantSettingsUseCase;
    }

    @GetMapping("/register")
    public String showRegistrationForm(@RequestHeader("X-Tenant-Id") UUID tenantId, Model model) {
        String loginMode = resolveLoginMode(tenantId);
        if ("OIDC".equals(loginMode)) {
            return "redirect:/auth/login";
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
            return "redirect:/auth/login";
        }
        model.addAttribute("loginMode", loginMode);
        model.addAttribute("username", username);
        model.addAttribute("email", email);
        model.addAttribute("displayName", displayName);

        if (!password.equals(passwordConfirm)) {
            model.addAttribute("error", "Die Passwörter stimmen nicht überein.");
            return "register";
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            model.addAttribute("error", "Das Passwort muss mindestens " + MIN_PASSWORD_LENGTH + " Zeichen lang sein.");
            return "register";
        }

        if (username.isBlank() || email.isBlank()) {
            model.addAttribute("error", "Benutzername und E-Mail sind Pflichtfelder.");
            return "register";
        }

        try {
            userProfileUseCase.register(
                    new RegisterUserCommand(username, password, email, displayName, new TenantId(tenantId)));
            return "registration-success";
        } catch (UserAlreadyExistsException e) {
            model.addAttribute("error", "Benutzername oder E-Mail-Adresse ist bereits vergeben.");
            return "register";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        } catch (Exception e) {
            LOG.error("Registration failed for user '{}'", username, e);
            model.addAttribute("error", "Die Registrierung ist fehlgeschlagen. Bitte versuchen Sie es erneut.");
            return "register";
        }
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
}
