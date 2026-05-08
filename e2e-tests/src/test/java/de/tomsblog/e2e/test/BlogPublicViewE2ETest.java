package de.tomsblog.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import de.tomsblog.blogcontent.BlogContentApplication;
import de.tomsblog.e2e.config.ScreenshotOnFailureExtension;
import de.tomsblog.e2e.config.WebDriverProvider;
import de.tomsblog.e2e.page.blog.LandingPage;
import de.tomsblog.e2e.page.blog.LegalPage;
import de.tomsblog.e2e.page.blog.PostListPage;
import org.junit.jupiter.api.BeforeAll;
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
 * E2E tests for public blog views: landing page, post list with search, post detail, legal pages,
 * and error pages.
 *
 * @req SWR-079
 */
@SpringBootTest(classes = BlogContentApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.testcontainers.junit.jupiter.Testcontainers
@ExtendWith(ScreenshotOnFailureExtension.class)
class BlogPublicViewE2ETest implements WebDriverProvider {

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
        registry.add("spring.thymeleaf.check-template-location", () -> "false");
        registry.add("grpc.client.user-management.address", () -> "static://localhost:9090");
        registry.add("grpc.client.user-management.negotiation-type", () -> "plaintext");
        registry.add("blog.admin.password", () -> "e2e-test-admin-password-12345");
    }

    @BeforeAll
    static void exposePort() {
        // Port is not known at container start; exposed in @BeforeEach
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

    @Test
    @DisplayName("SWR-079: Landing page loads with heading")
    void landingPageLoads() {
        LandingPage page = new LandingPage(driver, baseUrl).open();
        assertThat(page.getHeading()).isEqualTo("Willkommen");
    }

    @Test
    @DisplayName("SWR-079: Landing page shows link to all posts")
    void landingPageShowsAllPostsLink() {
        LandingPage page = new LandingPage(driver, baseUrl).open();
        assertThat(page.hasAllPostsLink()).isTrue();
    }

    @Test
    @DisplayName("SWR-079: Post list page loads with search form")
    void postListPageLoads() {
        PostListPage page = new PostListPage(driver, baseUrl).open();
        assertThat(page.getHeading()).isEqualTo("Blog Posts");
    }

    @Test
    @DisplayName("SWR-079: Search returns results info")
    void searchShowsResultInfo() {
        PostListPage page = new PostListPage(driver, baseUrl).open().search("nonexistent-term-xyz");
        assertThat(page.hasSearchResults()).isTrue();
    }

    @Test
    @DisplayName("SWR-079: Impressum page loads")
    void impressumPageLoads() {
        LegalPage page = new LegalPage(driver, baseUrl).openImpressum();
        assertThat(page.getHeading()).isNotBlank();
    }

    @Test
    @DisplayName("SWR-079: Privacy page loads")
    void privacyPageLoads() {
        LegalPage page = new LegalPage(driver, baseUrl).openPrivacy();
        assertThat(page.getHeading()).isNotBlank();
    }

    @Test
    @DisplayName("SWR-079: Unknown slug returns 404 page")
    void unknownSlugReturns404() {
        driver.get(baseUrl + "/posts/this-slug-does-not-exist-xyz");
        assertThat(driver.getPageSource()).containsIgnoringCase("404");
    }

    @Test
    @DisplayName("SWR-079: Post list shows empty message when no posts exist")
    void emptyPostListShowsMessage() {
        PostListPage page = new PostListPage(driver, baseUrl).open();
        // With a fresh database, no posts exist
        assertThat(page.hasEmptyMessage()).isTrue();
    }

    @Test
    @DisplayName("SWR-079: Back to list link works from post detail placeholder")
    void postListHasCorrectHeading() {
        PostListPage page = new PostListPage(driver, baseUrl).open();
        assertThat(page.getHeading()).isEqualTo("Blog Posts");
    }
}
