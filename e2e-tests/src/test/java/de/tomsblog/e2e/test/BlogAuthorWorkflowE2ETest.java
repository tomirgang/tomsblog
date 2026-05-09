package de.tomsblog.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import de.tomsblog.blogcontent.BlogContentApplication;
import de.tomsblog.e2e.config.ScreenshotOnFailureExtension;
import de.tomsblog.e2e.config.WebDriverProvider;
import de.tomsblog.e2e.page.blog.PostListPage;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 * E2E tests for authenticated blog author workflows: post creation and editing.
 *
 * @req SWR-080
 */
@SpringBootTest(classes = BlogContentApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.testcontainers.junit.jupiter.Testcontainers
@ExtendWith(ScreenshotOnFailureExtension.class)
class BlogAuthorWorkflowE2ETest implements WebDriverProvider {

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
        // Auto-configured UserDetailsService for HTTP Basic auth in blog-content
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
    @DisplayName("SWR-080: Post creation form loads for unauthenticated users redirecting to login")
    void postCreationRequiresAuthentication() {
        clearAuthentication();
        driver.get(baseUrl + "/posts/new");
        // Unauthenticated access to /posts/new should redirect to login
        assertThat(driver.getCurrentUrl()).contains("login");
    }

    @Test
    @DisplayName("SWR-080: Post list does not show new-post button for anonymous users")
    void postListHidesNewPostButtonForAnonymous() {
        clearAuthentication();
        PostListPage page = new PostListPage(driver, baseUrl).open();
        assertThat(page.hasNewPostButton()).isFalse();
    }

    @Test
    @DisplayName("SWR-080: Post form has required fields when authenticated")
    void postFormStructureWhenAuthenticated() {
        driver.get(baseUrl + "/posts/new");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("title")));
        assertThat(driver.findElement(By.id("title")).isDisplayed()).isTrue();
        assertThat(driver.findElement(By.id("content")).isDisplayed()).isTrue();
        assertThat(driver.findElement(By.id("contentType")).isDisplayed()).isTrue();
        assertThat(driver.findElement(By.id("locale")).isDisplayed()).isTrue();
        assertThat(driver.findElement(By.cssSelector("main button[type='submit']"))
                        .isDisplayed())
                .isTrue();
    }

    @Test
    @DisplayName("SWR-080: Post can be created via form submission")
    void postCreationViaForm() {
        driver.get(baseUrl + "/posts/new");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("title")));
        driver.findElement(By.id("title")).sendKeys("E2E Test Post Title");
        driver.findElement(By.id("content")).sendKeys("# Hello World\n\nThis is a test post.");
        new Select(driver.findElement(By.id("locale"))).selectByValue("de");
        // Submit the form
        driver.findElement(By.cssSelector("main button[type='submit']")).click();
        wait.until(ExpectedConditions.and(
                ExpectedConditions.urlContains("/posts"),
                ExpectedConditions.not(ExpectedConditions.urlContains("/new")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/edit")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/preview"))));
        // After creation, should redirect to post list
        assertThat(driver.getCurrentUrl()).contains("/posts");
    }

    @Test
    @DisplayName("SWR-080: Created post appears in post list for authenticated user")
    void createdPostVisibleInList() {
        // Create a post first
        driver.get(baseUrl + "/posts/new");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("title")));
        driver.findElement(By.id("title")).sendKeys("Visible Post");
        driver.findElement(By.id("content")).sendKeys("Content for visible post");
        new Select(driver.findElement(By.id("locale"))).selectByValue("de");
        driver.findElement(By.cssSelector("main button[type='submit']")).click();
        wait.until(ExpectedConditions.and(
                ExpectedConditions.urlContains("/posts"),
                ExpectedConditions.not(ExpectedConditions.urlContains("/new")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/edit")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/preview"))));
        // Verify it appears in the list (authenticated users see all posts)
        assertThat(driver.getPageSource()).contains("Visible Post");
    }

    @Test
    @DisplayName("SWR-080: Post edit form loads with existing post data")
    void postEditFormLoadsWithData() {
        // Create a post first
        driver.get(baseUrl + "/posts/new");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("title")));
        driver.findElement(By.id("title")).sendKeys("Editable Post");
        driver.findElement(By.id("content")).sendKeys("Original content");
        new Select(driver.findElement(By.id("locale"))).selectByValue("de");
        driver.findElement(By.cssSelector("main button[type='submit']")).click();
        wait.until(ExpectedConditions.and(
                ExpectedConditions.urlContains("/posts"),
                ExpectedConditions.not(ExpectedConditions.urlContains("/new")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/edit")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/preview"))));
        // Find the edit link for the created post
        var editLink = driver.findElement(
                By.xpath("//article[.//h2[contains(.,'Editable Post')]]//a[contains(@href,'/edit')]"));
        String editUrl = editLink.getDomAttribute("href");
        driver.get(baseUrl + editUrl.substring(editUrl.indexOf("/posts/")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("title")));
        // Verify form is pre-filled
        assertThat(driver.findElement(By.id("title")).getDomProperty("value")).isEqualTo("Editable Post");
        assertThat(driver.findElement(By.id("content")).getDomProperty("value")).contains("Original content");
    }

    @Test
    @DisplayName("SWR-080: Post can be updated via form submission")
    void postUpdateViaForm() {
        // Create a post first
        driver.get(baseUrl + "/posts/new");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("title")));
        driver.findElement(By.id("title")).sendKeys("Update Me");
        driver.findElement(By.id("content")).sendKeys("Before update");
        new Select(driver.findElement(By.id("locale"))).selectByValue("de");
        driver.findElement(By.cssSelector("main button[type='submit']")).click();
        wait.until(ExpectedConditions.and(
                ExpectedConditions.urlContains("/posts"),
                ExpectedConditions.not(ExpectedConditions.urlContains("/new")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/edit")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/preview"))));
        // Navigate to edit page
        var editLink =
                driver.findElement(By.xpath("//article[.//h2[contains(.,'Update Me')]]//a[contains(@href,'/edit')]"));
        String editUrl = editLink.getDomAttribute("href");
        driver.get(baseUrl + editUrl.substring(editUrl.indexOf("/posts/")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("title")));
        // Clear and update the content
        driver.findElement(By.id("content")).clear();
        driver.findElement(By.id("content")).sendKeys("After update");
        driver.findElement(By.cssSelector("main button[type='submit']")).click();
        wait.until(ExpectedConditions.and(
                ExpectedConditions.urlContains("/posts"),
                ExpectedConditions.not(ExpectedConditions.urlContains("/new")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/edit")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/preview"))));
        // Verify redirect to post list after update
        assertThat(driver.getCurrentUrl()).contains("/posts");
    }

    @Test
    @DisplayName("SWR-080: Post can be published")
    void postPublishAction() {
        // Create a post
        driver.get(baseUrl + "/posts/new");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("title")));
        driver.findElement(By.id("title")).sendKeys("Publishable Post");
        driver.findElement(By.id("content")).sendKeys("Content to publish");
        new Select(driver.findElement(By.id("locale"))).selectByValue("de");
        driver.findElement(By.cssSelector("main button[type='submit']")).click();
        wait.until(ExpectedConditions.and(
                ExpectedConditions.urlContains("/posts"),
                ExpectedConditions.not(ExpectedConditions.urlContains("/new")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/edit")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/preview"))));
        // Find and click the publish button/link
        var publishForm = driver.findElement(
                By.xpath("//article[.//h2[contains(.,'Publishable Post')]]//form[contains(@action,'/publish')]"));
        publishForm.findElement(By.cssSelector("button[type='submit']")).click();
        wait.until(ExpectedConditions.and(
                ExpectedConditions.urlContains("/posts"),
                ExpectedConditions.not(ExpectedConditions.urlContains("/new")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/edit")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/preview"))));
        assertThat(driver.getCurrentUrl()).contains("/posts");
    }

    @Test
    @DisplayName("SWR-080: Draft post can be previewed")
    void postPreviewAction() {
        // Create a post
        driver.get(baseUrl + "/posts/new");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("title")));
        driver.findElement(By.id("title")).sendKeys("Preview Post");
        driver.findElement(By.id("content")).sendKeys("Preview content here");
        new Select(driver.findElement(By.id("locale"))).selectByValue("de");
        driver.findElement(By.cssSelector("main button[type='submit']")).click();
        wait.until(ExpectedConditions.and(
                ExpectedConditions.urlContains("/posts"),
                ExpectedConditions.not(ExpectedConditions.urlContains("/new")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/edit")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/preview"))));
        // Navigate to edit page first (preview link is only on edit form)
        var editLink = driver.findElement(
                By.xpath("//article[.//h2[contains(.,'Preview Post')]]//a[contains(@href,'/edit')]"));
        String editUrl = editLink.getDomAttribute("href");
        driver.get(baseUrl + editUrl.substring(editUrl.indexOf("/posts/")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("title")));
        // Find the preview link on the edit form
        var previewLink = driver.findElement(By.xpath("//a[contains(@href,'/preview')]"));
        String previewUrl = previewLink.getDomAttribute("href");
        driver.get(baseUrl + previewUrl.substring(previewUrl.indexOf("/posts/")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("article")));
        assertThat(driver.getPageSource()).contains("Preview Post");
    }

    @Test
    @DisplayName("SWR-079: Published post is visible on public post detail page")
    void publishedPostVisibleOnPublicPage() {
        // Create and publish a post
        driver.get(baseUrl + "/posts/new");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("title")));
        driver.findElement(By.id("title")).sendKeys("Public Post");
        driver.findElement(By.id("content")).sendKeys("This is publicly visible");
        new Select(driver.findElement(By.id("locale"))).selectByValue("de");
        driver.findElement(By.cssSelector("main button[type='submit']")).click();
        wait.until(ExpectedConditions.and(
                ExpectedConditions.urlContains("/posts"),
                ExpectedConditions.not(ExpectedConditions.urlContains("/new")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/edit")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/preview"))));
        // Publish the post
        var publishForm = driver.findElement(
                By.xpath("//article[.//h2[contains(.,'Public Post')]]//form[contains(@action,'/publish')]"));
        publishForm.findElement(By.cssSelector("button[type='submit']")).click();
        wait.until(ExpectedConditions.and(
                ExpectedConditions.urlContains("/posts"),
                ExpectedConditions.not(ExpectedConditions.urlContains("/new")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/edit")),
                ExpectedConditions.not(ExpectedConditions.urlContains("/preview"))));
        // Now access as anonymous user via slug
        driver.get(baseUrl + "/posts/public-post");
        assertThat(driver.getPageSource()).contains("Public Post");
        assertThat(driver.getPageSource()).contains("publicly visible");
    }
}
