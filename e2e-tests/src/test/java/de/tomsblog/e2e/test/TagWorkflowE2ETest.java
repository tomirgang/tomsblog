package de.tomsblog.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import de.tomsblog.blogcontent.BlogContentApplication;
import de.tomsblog.e2e.config.ScreenshotOnFailureExtension;
import de.tomsblog.e2e.config.WebDriverProvider;
import de.tomsblog.e2e.page.admin.TagAdminPage;
import de.tomsblog.e2e.page.blog.PostDetailPage;
import de.tomsblog.e2e.page.blog.PostListPage;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
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
 * E2E tests for tag management and tag UI integration with posts.
 *
 * @req SWR-084
 * @req SWR-085
 * @req SWR-086
 */
@SpringBootTest(classes = BlogContentApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.testcontainers.junit.jupiter.Testcontainers
@ExtendWith(ScreenshotOnFailureExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TagWorkflowE2ETest implements WebDriverProvider {

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
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/blog");
        registry.add("spring.session.store-type", () -> "none");
        registry.add("spring.data.redis.repositories.enabled", () -> "false");
        registry.add(
                "spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration");
        registry.add("spring.thymeleaf.check-template-location", () -> "false");
        registry.add("grpc.client.user-management.address", () -> "static://localhost:9090");
        registry.add("grpc.client.user-management.negotiation-type", () -> "plaintext");
        registry.add("grpc.server.port", () -> "0");
        registry.add("blog.admin.password", () -> "e2e-test-admin-password-12345");
        registry.add("spring.security.user.name", () -> "author");
        registry.add("spring.security.user.password", () -> "e2e-author-pass");
        registry.add("spring.security.user.roles", () -> "AUTHOR,ADMIN");
    }

    @BeforeEach
    void setUp() {
        Testcontainers.exposeHostPorts(port);
        baseUrl = "http://host.testcontainers.internal:" + port;
        driver = new Augmenter().augment(new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions()));
        authenticateDriver();
    }

    private void authenticateDriver() {
        String credentials = Base64.getEncoder().encodeToString("author:e2e-author-pass".getBytes());
        var devTools = ((HasDevTools) driver).getDevTools();
        devTools.createSession();
        devTools.send(new org.openqa.selenium.devtools.Command<>("Network.enable", Map.of()));
        devTools.send(new org.openqa.selenium.devtools.Command<>(
                "Network.setExtraHTTPHeaders", Map.of("headers", Map.of("Authorization", "Basic " + credentials))));
    }

    private void clearAuthentication() {
        var devTools = ((HasDevTools) driver).getDevTools();
        devTools.send(
                new org.openqa.selenium.devtools.Command<>("Network.setExtraHTTPHeaders", Map.of("headers", Map.of())));
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
    @DisplayName("SWR-084: Tag admin page requires authentication")
    void tagAdminRequiresAuth() {
        clearAuthentication();
        driver.get(baseUrl + "/admin/tags");
        assertThat(driver.getCurrentUrl()).contains("login");
    }

    @Test
    @DisplayName("SWR-084: Tag admin page loads for authenticated users")
    void tagAdminLoadsAuthenticated() {
        TagAdminPage page = new TagAdminPage(driver, baseUrl).open();
        assertThat(page.getHeading()).isEqualTo("Tag-Verwaltung");
    }

    @Test
    @Order(1)
    @DisplayName("SWR-084: Tag admin shows empty message when no tags exist")
    void tagAdminShowsEmptyMessage() {
        TagAdminPage page = new TagAdminPage(driver, baseUrl).open();
        assertThat(page.hasEmptyMessage()).isTrue();
        assertThat(page.hasTagTable()).isFalse();
    }

    @Test
    @Order(2)
    @DisplayName("SWR-084: Tag can be created via admin page")
    void tagCreation() {
        TagAdminPage page = new TagAdminPage(driver, baseUrl).open();
        page.createTag("Java");

        assertThat(page.getTagNames()).contains("Java");
        assertThat(page.getTagSlugs()).contains("java");
        assertThat(page.hasTagTable()).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("SWR-084: Tag can be renamed via admin page")
    void tagRenaming() {
        TagAdminPage page = new TagAdminPage(driver, baseUrl).open();
        page.createTag("Jva");
        page.renameTag("Jva", "JvaCorrected");

        assertThat(page.getTagNames()).contains("JvaCorrected");
        assertThat(page.getTagNames()).doesNotContain("Jva");
    }

    @Test
    @Order(4)
    @DisplayName("SWR-084: Tag can be deleted via admin page")
    void tagDeletion() {
        TagAdminPage page = new TagAdminPage(driver, baseUrl).open();
        page.createTag("Temporary");
        assertThat(page.getTagNames()).contains("Temporary");

        page.deleteTag("Temporary");
        assertThat(page.getTagNames()).doesNotContain("Temporary");
    }

    @Test
    @DisplayName("SWR-085: Post form shows tag section with available tags")
    void postFormShowsTagSection() {
        // Create a tag first
        new TagAdminPage(driver, baseUrl).open().createTag("Spring");

        // Navigate to new post form
        driver.get(baseUrl + "/posts/new");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("title")));

        assertThat(driver.getPageSource()).contains("Tags");
        assertThat(driver.getPageSource()).contains("Spring");
        assertThat(driver.findElement(By.id("newTagName")).isDisplayed()).isTrue();
    }

    @Test
    @DisplayName("SWR-085: Post can be created with existing tag selected")
    void postCreationWithExistingTag() {
        // Create a tag
        new TagAdminPage(driver, baseUrl).open().createTag("Kotlin");

        // Create a post with the tag selected
        driver.get(baseUrl + "/posts/new");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("title")));
        driver.findElement(By.id("title")).sendKeys("Tagged Post");
        driver.findElement(By.id("content")).sendKeys("Content with tag");
        new Select(driver.findElement(By.id("locale"))).selectByValue("de");

        // Select the tag checkbox
        var checkboxes = driver.findElements(By.cssSelector("input[name='tagIds']"));
        for (var cb : checkboxes) {
            String label = cb.findElement(By.xpath("./following-sibling::span")).getText();
            if (label.equals("Kotlin")) {
                cb.click();
                break;
            }
        }

        driver.findElement(By.cssSelector("main button[type='submit']")).click();
        wait.until(ExpectedConditions.and(
                ExpectedConditions.urlContains("/posts"),
                ExpectedConditions.not(ExpectedConditions.urlContains("/new")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/edit")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/preview"))));
        assertThat(driver.getCurrentUrl()).contains("/posts");
    }

    @Test
    @DisplayName("SWR-085: Post can be created with inline new tag")
    void postCreationWithNewInlineTag() {
        driver.get(baseUrl + "/posts/new");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("title")));
        driver.findElement(By.id("title")).sendKeys("Inline Tag Post");
        driver.findElement(By.id("content")).sendKeys("Content with inline tag");
        new Select(driver.findElement(By.id("locale"))).selectByValue("de");
        driver.findElement(By.id("newTagName")).sendKeys("DevOps");

        driver.findElement(By.cssSelector("main button[type='submit']")).click();
        wait.until(ExpectedConditions.and(
                ExpectedConditions.urlContains("/posts"),
                ExpectedConditions.not(ExpectedConditions.urlContains("/new")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/edit")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/preview"))));
        assertThat(driver.getCurrentUrl()).contains("/posts");

        // Verify the tag was created in the admin page
        TagAdminPage tagPage = new TagAdminPage(driver, baseUrl).open();
        assertThat(tagPage.getTagNames()).contains("DevOps");
    }

    @Test
    @DisplayName("SWR-086: Published post with tag shows tag labels on detail page")
    void publishedPostShowsTagsOnDetailPage() {
        // Create a tag
        new TagAdminPage(driver, baseUrl).open().createTag("Testing");

        // Create a post with the tag
        driver.get(baseUrl + "/posts/new");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("title")));
        driver.findElement(By.id("title")).sendKeys("Tag Display Post");
        driver.findElement(By.id("content")).sendKeys("Testing tag display");
        new Select(driver.findElement(By.id("locale"))).selectByValue("de");

        var checkboxes = driver.findElements(By.cssSelector("input[name='tagIds']"));
        for (var cb : checkboxes) {
            String label = cb.findElement(By.xpath("./following-sibling::span")).getText();
            if (label.equals("Testing")) {
                cb.click();
                break;
            }
        }

        driver.findElement(By.cssSelector("main button[type='submit']")).click();
        wait.until(ExpectedConditions.and(
                ExpectedConditions.urlContains("/posts"),
                ExpectedConditions.not(ExpectedConditions.urlContains("/new")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/edit")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/preview"))));

        // Publish the post
        var publishForm = driver.findElement(
                By.xpath("//article[.//h2[contains(.,'Tag Display Post')]]//form[contains(@action,'/publish')]"));
        publishForm.findElement(By.cssSelector("button[type='submit']")).click();
        wait.until(ExpectedConditions.and(
                ExpectedConditions.urlContains("/posts"),
                ExpectedConditions.not(ExpectedConditions.urlContains("/new")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/edit")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/preview"))));

        // View the published post as anonymous user
        PostDetailPage detailPage = new PostDetailPage(driver, baseUrl).open("tag-display-post");
        assertThat(detailPage.hasTagLabels()).isTrue();
        assertThat(detailPage.getTagNames()).contains("Testing");
    }

    @Test
    @DisplayName("SWR-086: Published post with tag shows tag labels in post list")
    void publishedPostShowsTagsInList() {
        // Create a tag
        new TagAdminPage(driver, baseUrl).open().createTag("Architecture");

        // Create and publish a post with the tag
        driver.get(baseUrl + "/posts/new");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("title")));
        driver.findElement(By.id("title")).sendKeys("Listed Tag Post");
        driver.findElement(By.id("content")).sendKeys("<p>Post with tags in list</p>");
        new Select(driver.findElement(By.id("locale"))).selectByValue("de");

        var checkboxes = driver.findElements(By.cssSelector("input[name='tagIds']"));
        for (var cb : checkboxes) {
            String label = cb.findElement(By.xpath("./following-sibling::span")).getText();
            if (label.equals("Architecture")) {
                cb.click();
                break;
            }
        }

        driver.findElement(By.cssSelector("main button[type='submit']")).click();
        wait.until(ExpectedConditions.and(
                ExpectedConditions.urlContains("/posts"),
                ExpectedConditions.not(ExpectedConditions.urlContains("/new")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/edit")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/preview"))));

        // Publish the post
        var publishForm = driver.findElement(
                By.xpath("//article[.//h2[contains(.,'Listed Tag Post')]]//form[contains(@action,'/publish')]"));
        publishForm.findElement(By.cssSelector("button[type='submit']")).click();
        wait.until(ExpectedConditions.and(
                ExpectedConditions.urlContains("/posts"),
                ExpectedConditions.not(ExpectedConditions.urlContains("/new")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/edit")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/preview"))));

        // Check public post list
        PostListPage listPage = new PostListPage(driver, baseUrl).open();
        assertThat(listPage.hasTagLabels()).isTrue();
        assertThat(listPage.getTagLabelsForPost("Listed Tag Post")).contains("Architecture");
    }

    @Test
    @DisplayName("SWR-084: Navigation includes Tags link for authenticated users")
    void navigationIncludesTagsLink() {
        driver.get(baseUrl + "/posts");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h1")));

        assertThat(driver.findElement(By.linkText("Tags")).isDisplayed()).isTrue();
    }
}
