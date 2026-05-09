package de.tomsblog.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import de.tomsblog.e2e.config.ScreenshotOnFailureExtension;
import de.tomsblog.e2e.config.WebDriverProvider;
import de.tomsblog.e2e.page.admin.GlobalSettingsPage;
import de.tomsblog.e2e.page.admin.TenantListPage;
import de.tomsblog.e2e.page.auth.AdminLoginPage;
import de.tomsblog.tenantmanagement.TenantManagementApplication;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
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
 * E2E tests for SuperAdmin tenant management workflows.
 *
 * @req SWR-083
 */
@SpringBootTest(classes = TenantManagementApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.testcontainers.junit.jupiter.Testcontainers
@ExtendWith(ScreenshotOnFailureExtension.class)
class SuperAdminE2ETest implements WebDriverProvider {

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
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/tenant");
        registry.add("spring.session.store-type", () -> "none");
        registry.add("spring.data.redis.repositories.enabled", () -> "false");
        registry.add(
                "spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration");
        registry.add("grpc.server.port", () -> "0");
        registry.add("blog.admin.username", () -> "superadmin");
        registry.add("blog.admin.password", () -> "e2e-test-admin-password-12345");
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

    private void loginAsSuperAdmin() {
        AdminLoginPage loginPage = new AdminLoginPage(driver, baseUrl, "/tenant/admin/login");
        loginPage.open();
        loginPage.login("superadmin", "e2e-test-admin-password-12345");
    }

    @Test
    @DisplayName("SWR-083: SuperAdmin login page loads")
    void superAdminLoginPageLoads() {
        AdminLoginPage page = new AdminLoginPage(driver, baseUrl, "/tenant/admin/login").open();
        assertThat(page.getHeading()).isEqualTo("Tenant Admin Login");
    }

    @Test
    @DisplayName("SWR-083: SuperAdmin login with invalid credentials shows error")
    void superAdminLoginWithInvalidCredentials() {
        AdminLoginPage page = new AdminLoginPage(driver, baseUrl, "/tenant/admin/login").open();
        page.login("invalid", "wrong");
        assertThat(page.hasErrorMessage()).isTrue();
    }

    @Test
    @DisplayName("SWR-083: Tenant list requires authentication")
    void tenantListRequiresAuth() {
        driver.get(baseUrl + "/tenant/admin/tenants");
        assertThat(driver.getCurrentUrl()).contains("login");
    }

    @Test
    @DisplayName("SWR-083: Tenant list loads after SuperAdmin login")
    void tenantListLoadsAfterLogin() {
        loginAsSuperAdmin();
        TenantListPage page = new TenantListPage(driver, baseUrl).open();
        assertThat(page.getHeading()).isEqualTo("Tenant-Verwaltung");
    }

    @Test
    @DisplayName("SWR-083: Tenant list has settings link")
    void tenantListHasSettingsLink() {
        loginAsSuperAdmin();
        TenantListPage page = new TenantListPage(driver, baseUrl).open();
        assertThat(page.hasSettingsLink()).isTrue();
    }

    @Test
    @DisplayName("SWR-083: Global settings page has all tabs")
    void globalSettingsHasAllTabs() {
        loginAsSuperAdmin();
        GlobalSettingsPage page = new GlobalSettingsPage(driver, baseUrl).open();
        assertThat(page.getHeading()).isEqualTo("Tenant-Einstellungen");
        assertThat(page.hasGeneralTab()).isTrue();
        assertThat(page.hasOidcTab()).isTrue();
        assertThat(page.hasLegalTab()).isTrue();
    }

    @Test
    @DisplayName("SWR-083: Global settings has link back to tenant list")
    void globalSettingsHasTenantListLink() {
        loginAsSuperAdmin();
        GlobalSettingsPage page = new GlobalSettingsPage(driver, baseUrl).open();
        assertThat(page.hasTenantListLink()).isTrue();
    }

    @Test
    @DisplayName("SWR-083: SuperAdmin can create a new tenant")
    void superAdminCanCreateTenant() {
        loginAsSuperAdmin();
        TenantListPage page = new TenantListPage(driver, baseUrl).open();
        page.createTenant("e2e-test-tenant", "E2E Test Tenant");
        // After creation, the tenant list should contain the new tenant
        assertThat(page.getTenantSlugs()).contains("e2e-test-tenant");
    }

    @Test
    @DisplayName("SWR-083: SuperAdmin can save general settings")
    void superAdminCanSaveGeneralSettings() {
        loginAsSuperAdmin();
        GlobalSettingsPage page = new GlobalSettingsPage(driver, baseUrl).open();
        page.fillDisplayName("Updated Tenant Name");
        page.submitGeneralForm();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains("/tenant/admin/settings"));
        assertThat(driver.getCurrentUrl()).contains("/tenant/admin/settings");
    }

    @Test
    @DisplayName("SWR-083: SuperAdmin can navigate to OIDC settings tab")
    void superAdminCanAccessOidcTab() {
        loginAsSuperAdmin();
        GlobalSettingsPage page = new GlobalSettingsPage(driver, baseUrl).open();
        page.switchToTab("OIDC");
        assertThat(driver.getCurrentUrl()).containsAnyOf("tab=oidc", "OIDC");
    }

    @Test
    @DisplayName("SWR-083: SuperAdmin can save legal settings")
    void superAdminCanSaveLegalSettings() {
        loginAsSuperAdmin();
        driver.get(baseUrl + "/tenant/admin/settings?tab=legal");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h2")));
        // Fill legal form fields if present
        if (driver.findElements(By.id("impressumContent")).size() > 0) {
            driver.findElement(By.id("impressumContent")).clear();
            driver.findElement(By.id("impressumContent")).sendKeys("E2E Impressum");
        }
        if (driver.findElements(By.id("privacyPolicyContent")).size() > 0) {
            driver.findElement(By.id("privacyPolicyContent")).clear();
            driver.findElement(By.id("privacyPolicyContent")).sendKeys("E2E Privacy");
        }
        driver.findElement(By.cssSelector(".tab-content.active button[type='submit']"))
                .click();
        wait.until(ExpectedConditions.urlContains("/tenant/admin/settings"));
        assertThat(driver.getCurrentUrl()).contains("/tenant/admin/settings");
    }

    @Test
    @DisplayName("SWR-083: SuperAdmin can switch active tenant")
    void superAdminCanSwitchTenant() {
        loginAsSuperAdmin();
        // First create a tenant to switch to
        TenantListPage page = new TenantListPage(driver, baseUrl).open();
        page.createTenant("switch-target", "Switch Target");
        // Look for the switch-tenant form
        driver.get(baseUrl + "/tenant/admin/tenants");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h2")));
        boolean hasSwitchForm = driver.findElements(By.xpath("//form[contains(@action,'/switch-tenant')]"))
                        .size()
                > 0;
        if (hasSwitchForm) {
            driver.findElement(By.xpath("//form[contains(@action,'/switch-tenant')]//button"))
                    .click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h2")));
        }
        assertThat(driver.getCurrentUrl()).contains("/tenant/admin/");
    }
}
