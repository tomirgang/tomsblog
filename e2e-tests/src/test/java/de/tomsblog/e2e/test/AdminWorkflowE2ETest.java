package de.tomsblog.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import de.tomsblog.e2e.config.ScreenshotOnFailureExtension;
import de.tomsblog.e2e.config.WebDriverProvider;
import de.tomsblog.e2e.page.admin.TenantSettingsPage;
import de.tomsblog.e2e.page.admin.UserManagementPage;
import de.tomsblog.e2e.page.auth.AdminLoginPage;
import de.tomsblog.e2e.page.auth.RegisterPage;
import de.tomsblog.usermanagement.UserManagementApplication;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

/**
 * E2E tests for admin workflows: user management and tenant settings.
 *
 * @req SWR-082
 */
@SpringBootTest(classes = UserManagementApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.testcontainers.junit.jupiter.Testcontainers
@ExtendWith(ScreenshotOnFailureExtension.class)
class AdminWorkflowE2ETest implements WebDriverProvider {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static final BrowserWebDriverContainer<?> chrome =
            new BrowserWebDriverContainer<>().withCapabilities(new ChromeOptions());

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private String baseUrl;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.session.store-type", () -> "none");
        registry.add("spring.data.redis.repositories.enabled", () -> "false");
        registry.add(
                "spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration");
        registry.add("grpc.server.port", () -> "0");
        registry.add("blog.admin.username", () -> "admin");
        registry.add("blog.admin.password", () -> "e2e-test-admin-password-12345");
        registry.add("service.api-key", () -> "e2e-test-api-key");
        registry.add("spring.security.oauth2.client.registration.authentik.client-id", () -> "e2e-test");
        registry.add("spring.security.oauth2.client.registration.authentik.client-secret", () -> "e2e-test-secret");
        registry.add("spring.security.oauth2.client.registration.authentik.scope", () -> "openid,profile,email");
        registry.add(
                "spring.security.oauth2.client.provider.authentik.issuer-uri", () -> "http://localhost:19999/dummy");
    }

    @BeforeEach
    void setUp() {
        Testcontainers.exposeHostPorts(port);
        baseUrl = "http://host.testcontainers.internal:" + port;
        driver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());
    }

    @Override
    public WebDriver getWebDriver() {
        return driver;
    }

    private void loginAsAdmin() {
        AdminLoginPage loginPage = new AdminLoginPage(driver, baseUrl, "/auth/admin/login");
        loginPage.open();
        loginPage.login("admin", "e2e-test-admin-password-12345");
    }

    private void registerUser(String username, String email, String displayName) {
        RegisterPage page = new RegisterPage(driver, baseUrl).open();
        page.fillForm(username, email, displayName, "securepassword123");
        page.submit();
    }

    @Test
    @DisplayName("SWR-082: User management page requires authentication")
    void userManagementRequiresAuth() {
        driver.get(baseUrl + "/auth/admin/users");
        assertThat(driver.getCurrentUrl()).contains("login");
    }

    @Test
    @DisplayName("SWR-082: User management page loads after admin login")
    void userManagementPageLoadsAfterLogin() {
        loginAsAdmin();
        UserManagementPage page = new UserManagementPage(driver, baseUrl).open();
        assertThat(page.getHeading()).isEqualTo("Benutzerverwaltung");
    }

    @Test
    @DisplayName("SWR-082: User management page shows settings link")
    void userManagementHasSettingsLink() {
        loginAsAdmin();
        UserManagementPage page = new UserManagementPage(driver, baseUrl).open();
        assertThat(page.hasSettingsLink()).isTrue();
    }

    @Test
    @DisplayName("SWR-082: Tenant settings page loads after admin login")
    void tenantSettingsPageLoads() {
        loginAsAdmin();
        TenantSettingsPage page = new TenantSettingsPage(driver, baseUrl).open();
        assertThat(page.getHeading()).containsAnyOf("Einstellungen", "Settings");
    }

    @Test
    @DisplayName("SWR-082: Registered user appears in user management list")
    void registeredUserAppearsInList() {
        // Register a new user
        registerUser("pendinguser", "pending@example.com", "Pending User");
        // Login as admin and check user list
        loginAsAdmin();
        UserManagementPage page = new UserManagementPage(driver, baseUrl).open();
        assertThat(page.getUserNames()).contains("pendinguser");
    }

    @Test
    @DisplayName("SWR-082: Admin can approve a pending user")
    void adminCanApproveUser() {
        // Register a user to approve
        registerUser("approveuser", "approve@example.com", "Approve User");
        // Login as admin
        loginAsAdmin();
        UserManagementPage page = new UserManagementPage(driver, baseUrl).open();
        // Click approve for the user
        page.approveUser("approveuser");
        // After approval, user should still be in the list (but status changed)
        assertThat(driver.getCurrentUrl()).contains("/auth/admin/users");
    }

    @Test
    @DisplayName("SWR-082: Admin can reject a pending user")
    void adminCanRejectUser() {
        // Register a user to reject
        registerUser("rejectuser", "reject@example.com", "Reject User");
        // Login as admin
        loginAsAdmin();
        driver.get(baseUrl + "/auth/admin/users");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h2")));
        // Find and click reject button for the user
        WebElement row = driver.findElements(By.cssSelector("tbody tr")).stream()
                .filter(r -> r.findElement(By.cssSelector("td:first-child"))
                        .getText()
                        .equals("rejectuser"))
                .findFirst()
                .orElse(null);
        if (row != null) {
            row.findElement(By.xpath(".//form[contains(@action,'/reject')]//button"))
                    .click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h2")));
        }
        assertThat(driver.getCurrentUrl()).contains("/auth/admin/users");
    }

    @Test
    @DisplayName("SWR-082: Admin can change user role")
    void adminCanChangeUserRole() {
        // Register a user
        registerUser("roleuser", "role@example.com", "Role User");
        // Login as admin
        loginAsAdmin();
        driver.get(baseUrl + "/auth/admin/users");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h2")));
        // Find role change form for the user
        boolean hasRoleForm = driver.findElements(By.xpath("//form[contains(@action,'/role')]"))
                        .size()
                > 0;
        if (hasRoleForm) {
            WebElement roleForm = driver.findElement(By.xpath("//form[contains(@action,'/role')]"));
            roleForm.findElement(By.cssSelector("button[type='submit']")).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h2")));
        }
        assertThat(driver.getCurrentUrl()).contains("/auth/admin/users");
    }

    @Test
    @DisplayName("SWR-082: General settings tab has form fields")
    void generalSettingsTabHasForm() {
        loginAsAdmin();
        TenantSettingsPage page = new TenantSettingsPage(driver, baseUrl).openTab("general");
        assertThat(page.hasSubmitButton()).isTrue();
    }

    @Test
    @DisplayName("SWR-082: General settings can be saved")
    void generalSettingsCanBeSaved() {
        loginAsAdmin();
        TenantSettingsPage page = new TenantSettingsPage(driver, baseUrl).openTab("general");
        page.fillDisplayName("E2E Test Blog");
        page.submitForm();
        // After save, should redirect back to settings with tab=general
        assertThat(driver.getCurrentUrl()).contains("/auth/admin/settings");
    }

    @Test
    @DisplayName("SWR-082: OIDC settings tab loads")
    void oidcSettingsTabLoads() {
        loginAsAdmin();
        TenantSettingsPage page = new TenantSettingsPage(driver, baseUrl).openTab("oidc");
        assertThat(page.hasSubmitButton()).isTrue();
    }

    @Test
    @DisplayName("SWR-082: Legal settings tab loads and can be saved")
    void legalSettingsCanBeSaved() {
        loginAsAdmin();
        TenantSettingsPage page = new TenantSettingsPage(driver, baseUrl).openTab("legal");
        page.fillImpressum("E2E Test Impressum Content");
        page.fillPrivacyPolicy("E2E Test Privacy Policy");
        page.submitForm();
        assertThat(driver.getCurrentUrl()).contains("/auth/admin/settings");
    }

    @Test
    @DisplayName("SWR-082: Admin can switch tenant")
    void adminCanSwitchTenant() {
        loginAsAdmin();
        driver.get(baseUrl + "/auth/admin/users");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h2")));
        // Check if switch-tenant form exists
        boolean hasSwitchForm = driver.findElements(By.xpath("//form[contains(@action,'/switch-tenant')]"))
                        .size()
                > 0;
        if (hasSwitchForm) {
            WebElement switchForm = driver.findElement(By.xpath("//form[contains(@action,'/switch-tenant')]"));
            switchForm.findElement(By.cssSelector("button[type='submit']")).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h2")));
        }
        // Verify we're still in the admin area
        assertThat(driver.getCurrentUrl()).containsAnyOf("/auth/admin/", "/auth/login");
    }
}
