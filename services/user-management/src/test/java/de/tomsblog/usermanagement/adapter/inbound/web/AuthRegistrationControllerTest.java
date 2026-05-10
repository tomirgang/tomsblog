package de.tomsblog.usermanagement.adapter.inbound.web;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.RegisterUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.application.port.inbound.UserProfileUseCase;
import de.tomsblog.usermanagement.application.service.UserAlreadyExistsException;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

@DisplayName("SWR-059: AuthRegistrationController")
class AuthRegistrationControllerTest {

    private static final UUID TENANT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final TenantId TENANT_ID = TenantId.of(TENANT_UUID);

    private UserProfileUseCase userProfileUseCase;
    private TenantSettingsUseCase tenantSettingsUseCase;
    private AuthRegistrationController controller;

    @BeforeEach
    void setUp() {
        userProfileUseCase = mock(UserProfileUseCase.class);
        tenantSettingsUseCase = mock(TenantSettingsUseCase.class);
        controller = new AuthRegistrationController(userProfileUseCase, tenantSettingsUseCase);
    }

    private void setupLoginMode(LoginMode mode) {
        var settings = TenantSettings.reconstitute(
                TENANT_ID, mode, false, Set.of(), "Blog", null, null, null, null, null, null);
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(settings);
    }

    @Test
    @DisplayName("GET /register returns form when login mode is BOTH")
    void showFormWhenBoth() {
        setupLoginMode(LoginMode.BOTH);
        Model model = new ConcurrentModel();
        String view = controller.showRegistrationForm(TENANT_UUID, model);
        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("loginMode")).isEqualTo("BOTH");
    }

    @Test
    @DisplayName("GET /register redirects to login when OIDC only")
    void redirectWhenOidc() {
        setupLoginMode(LoginMode.OIDC);
        Model model = new ConcurrentModel();
        String view = controller.showRegistrationForm(TENANT_UUID, model);
        assertThat(view).isEqualTo("redirect:/auth/login");
    }

    @Test
    @DisplayName("POST /register succeeds with valid data")
    void registerSuccess() {
        setupLoginMode(LoginMode.BOTH);
        var profile = UserProfile.createInternal("newuser", "hash", "new@test.com", "New User");
        when(userProfileUseCase.register(any(RegisterUserCommand.class))).thenReturn(profile);

        Model model = new ConcurrentModel();
        String view = controller.register(
                TENANT_UUID, "newuser", "new@test.com", "New User", "securePassword1!", "securePassword1!", model);

        assertThat(view).isEqualTo("registration-success");
    }

    @Test
    @DisplayName("POST /register shows error when passwords don't match")
    void passwordMismatch() {
        setupLoginMode(LoginMode.BOTH);
        Model model = new ConcurrentModel();
        String view = controller.register(
                TENANT_UUID, "newuser", "new@test.com", "New User", "password12345", "different12345", model);

        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("error")).asString().contains("stimmen nicht überein");
    }

    @Test
    @DisplayName("POST /register shows error when password too short")
    void passwordTooShort() {
        setupLoginMode(LoginMode.BOTH);
        Model model = new ConcurrentModel();
        String view = controller.register(TENANT_UUID, "newuser", "new@test.com", "New User", "short", "short", model);

        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("error")).asString().contains("12 Zeichen");
    }

    @Test
    @DisplayName("POST /register shows error when password lacks complexity")
    void passwordNotComplex() {
        setupLoginMode(LoginMode.BOTH);
        Model model = new ConcurrentModel();
        String view = controller.register(
                TENANT_UUID, "newuser", "new@test.com", "New User", "alllowercaselong", "alllowercaselong", model);

        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("error")).asString().contains("Großbuchstaben");
    }

    @Test
    @DisplayName("POST /register shows error when username is blank")
    void blankUsername() {
        setupLoginMode(LoginMode.BOTH);
        Model model = new ConcurrentModel();
        String view = controller.register(
                TENANT_UUID, "", "new@test.com", null, "securePassword1!", "securePassword1!", model);

        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("error")).asString().contains("Pflichtfelder");
    }

    @Test
    @DisplayName("POST /register shows error when email is blank")
    void blankEmail() {
        setupLoginMode(LoginMode.BOTH);
        Model model = new ConcurrentModel();
        String view =
                controller.register(TENANT_UUID, "newuser", "", null, "securePassword1!", "securePassword1!", model);

        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("error")).asString().contains("Pflichtfelder");
    }

    @Test
    @DisplayName("POST /register shows error when user already exists")
    void userAlreadyExists() {
        setupLoginMode(LoginMode.BOTH);
        when(userProfileUseCase.register(any())).thenThrow(new UserAlreadyExistsException("exists"));

        Model model = new ConcurrentModel();
        String view = controller.register(
                TENANT_UUID, "existing", "ex@test.com", null, "securePassword1!", "securePassword1!", model);

        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("error")).asString().contains("bereits vergeben");
    }

    @Test
    @DisplayName("POST /register shows error on IllegalArgumentException")
    void illegalArgument() {
        setupLoginMode(LoginMode.BOTH);
        when(userProfileUseCase.register(any())).thenThrow(new IllegalArgumentException("Bad input"));

        Model model = new ConcurrentModel();
        String view = controller.register(
                TENANT_UUID, "newuser", "new@test.com", null, "securePassword1!", "securePassword1!", model);

        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("error")).isEqualTo("Bad input");
    }

    @Test
    @DisplayName("POST /register shows generic error on unexpected exception")
    void unexpectedException() {
        setupLoginMode(LoginMode.BOTH);
        when(userProfileUseCase.register(any())).thenThrow(new RuntimeException("Unexpected"));

        Model model = new ConcurrentModel();
        String view = controller.register(
                TENANT_UUID, "newuser", "new@test.com", null, "securePassword1!", "securePassword1!", model);

        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("error")).asString().contains("fehlgeschlagen");
    }

    @Test
    @DisplayName("POST /register redirects when OIDC only")
    void postRedirectWhenOidc() {
        setupLoginMode(LoginMode.OIDC);
        Model model = new ConcurrentModel();
        String view = controller.register(
                TENANT_UUID, "newuser", "new@test.com", null, "securePassword1!", "securePassword1!", model);
        assertThat(view).isEqualTo("redirect:/auth/login");
    }

    @Test
    @DisplayName("GET /register defaults to BOTH when settings are null")
    void showFormDefaultsBothWhenNull() {
        when(tenantSettingsUseCase.getSettings(any())).thenReturn(null);
        Model model = new ConcurrentModel();
        String view = controller.showRegistrationForm(TENANT_UUID, model);
        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("loginMode")).isEqualTo("BOTH");
    }

    @Test
    @DisplayName("GET /register defaults to BOTH on exception")
    void showFormDefaultsBothOnException() {
        when(tenantSettingsUseCase.getSettings(any())).thenThrow(new RuntimeException("DB down"));
        Model model = new ConcurrentModel();
        String view = controller.showRegistrationForm(TENANT_UUID, model);
        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("loginMode")).isEqualTo("BOTH");
    }

    @Test
    @DisplayName("POST /register shows error when password has no lowercase")
    void passwordNoLowercase() {
        setupLoginMode(LoginMode.BOTH);
        Model model = new ConcurrentModel();
        String view = controller.register(
                TENANT_UUID, "newuser", "new@test.com", "New User", "ABCDEFGHIJK123", "ABCDEFGHIJK123", model);

        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("error")).asString().contains("Großbuchstaben");
    }

    @Test
    @DisplayName("POST /register shows error when password has no digits")
    void passwordNoDigits() {
        setupLoginMode(LoginMode.BOTH);
        Model model = new ConcurrentModel();
        String view = controller.register(
                TENANT_UUID, "newuser", "new@test.com", "New User", "UpperLowerOnly", "UpperLowerOnly", model);

        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("error")).asString().contains("Großbuchstaben");
    }
}
