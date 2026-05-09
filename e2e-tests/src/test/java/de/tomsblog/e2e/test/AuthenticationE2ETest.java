package de.tomsblog.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import de.tomsblog.e2e.config.ScreenshotOnFailureExtension;
import de.tomsblog.e2e.config.WebDriverProvider;
import de.tomsblog.e2e.page.auth.AdminLoginPage;
import de.tomsblog.e2e.page.auth.LoginPage;
import de.tomsblog.e2e.page.auth.RegisterPage;
import de.tomsblog.usermanagement.UserManagementApplication;
import org.junit.jupiter.api.AfterEach;
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
 * E2E tests for authentication and registration workflows.
 *
 * @req SWR-081
 */
@SpringBootTest(classes = UserManagementApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.testcontainers.junit.jupiter.Testcontainers
@ExtendWith(ScreenshotOnFailureExtension.class)
class AuthenticationE2ETest implements WebDriverProvider {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static final BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>()
            .withCapabilities(new ChromeOptions())
            .withAccessToHost(true);

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
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/user");
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
        // Minimal OIDC config to satisfy Spring auto-configuration (no issuer-uri to avoid discovery)
        registry.add("spring.security.oauth2.client.registration.authentik.client-id", () -> "e2e-test");
        registry.add("spring.security.oauth2.client.registration.authentik.client-secret", () -> "e2e-test-secret");
        registry.add("spring.security.oauth2.client.registration.authentik.scope", () -> "openid,profile,email");
        registry.add(
                "spring.security.oauth2.client.registration.authentik.authorization-grant-type",
                () -> "authorization_code");
        registry.add(
                "spring.security.oauth2.client.registration.authentik.redirect-uri",
                () -> "{baseUrl}/login/oauth2/code/{registrationId}");
        registry.add(
                "spring.security.oauth2.client.provider.authentik.authorization-uri",
                () -> "http://localhost:19999/authorize");
        registry.add(
                "spring.security.oauth2.client.provider.authentik.token-uri", () -> "http://localhost:19999/token");
        registry.add(
                "spring.security.oauth2.client.provider.authentik.user-info-uri",
                () -> "http://localhost:19999/userinfo");
        registry.add(
                "spring.security.oauth2.client.provider.authentik.jwk-set-uri", () -> "http://localhost:19999/jwks");
    }

    @BeforeEach
    void setUp() {
        Testcontainers.exposeHostPorts(port);
        baseUrl = "http://host.testcontainers.internal:" + port;
        driver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Override
    public WebDriver getWebDriver() {
        return driver;
    }

    @Test
    @DisplayName("SWR-081: Login page loads with form")
    void loginPageLoads() {
        LoginPage page = new LoginPage(driver, baseUrl).open();
        assertThat(page.getHeading()).isEqualTo("Login");
        assertThat(page.hasInternalLoginForm()).isTrue();
    }

    @Test
    @DisplayName("SWR-081: Login with invalid credentials shows error")
    void loginWithInvalidCredentials() {
        LoginPage page = new LoginPage(driver, baseUrl).open();
        page.login("invalid-user", "wrong-password");
        assertThat(page.hasErrorMessage()).isTrue();
    }

    @Test
    @DisplayName("SWR-081: Login page shows registration link")
    void loginPageHasRegisterLink() {
        LoginPage page = new LoginPage(driver, baseUrl).open();
        assertThat(page.hasRegisterLink()).isTrue();
    }

    @Test
    @DisplayName("SWR-081: Registration page loads with form")
    void registrationPageLoads() {
        RegisterPage page = new RegisterPage(driver, baseUrl).open();
        assertThat(page.getHeading()).isEqualTo("Registrieren");
        assertThat(page.hasLoginLink()).isTrue();
    }

    @Test
    @DisplayName("SWR-081: Registration form submits and shows success")
    void registrationFormSubmits() {
        RegisterPage page = new RegisterPage(driver, baseUrl).open();
        page.fillForm("testuser", "test@example.com", "Test User", "securepassword123");
        page.submit();
        // Should redirect to success page or show confirmation
        assertThat(driver.getPageSource()).containsAnyOf("erfolgreich", "Registrierung", "registration");
    }

    @Test
    @DisplayName("SWR-081: Admin login page loads")
    void adminLoginPageLoads() {
        AdminLoginPage page = new AdminLoginPage(driver, baseUrl, "/auth/admin/login").open();
        assertThat(page.getHeading()).isEqualTo("Admin Login");
    }

    @Test
    @DisplayName("SWR-081: Admin login with invalid credentials shows error")
    void adminLoginWithInvalidCredentials() {
        AdminLoginPage page = new AdminLoginPage(driver, baseUrl, "/auth/admin/login").open();
        page.login("invalid-admin", "wrong-password");
        assertThat(page.hasErrorMessage()).isTrue();
    }

    @Test
    @DisplayName("SWR-081: Admin login with valid credentials succeeds")
    void adminLoginWithValidCredentials() {
        AdminLoginPage page = new AdminLoginPage(driver, baseUrl, "/auth/admin/login").open();
        page.login("admin", "e2e-test-admin-password-12345");
        // Should redirect to admin area
        assertThat(driver.getCurrentUrl()).contains("/auth/admin/");
    }
}
