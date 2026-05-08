package de.tomsblog.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import de.tomsblog.e2e.config.ScreenshotOnFailureExtension;
import de.tomsblog.e2e.config.WebDriverProvider;
import de.tomsblog.e2e.page.admin.UserManagementPage;
import de.tomsblog.e2e.page.auth.AdminLoginPage;
import de.tomsblog.usermanagement.UserManagementApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
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
        driver.get(baseUrl + "/auth/admin/settings");
        assertThat(driver.getPageSource()).containsAnyOf("Einstellungen", "Settings");
    }
}
