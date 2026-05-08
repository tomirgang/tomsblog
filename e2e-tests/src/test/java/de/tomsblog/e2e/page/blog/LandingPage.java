package de.tomsblog.e2e.page.blog;

import de.tomsblog.e2e.page.BasePage;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for the blog landing page (GET /).
 *
 * @req SWR-079
 */
public class LandingPage extends BasePage {

    public LandingPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public LandingPage open() {
        navigateTo("/");
        waitForElement(By.tagName("h1"));
        return this;
    }

    public String getHeading() {
        return getText(By.tagName("h1"));
    }

    public List<String> getFeaturedPostTitles() {
        return driver.findElements(By.cssSelector(".featured-posts .featured h3")).stream()
                .map(el -> el.getText().trim())
                .toList();
    }

    public List<String> getPostTitles() {
        return driver.findElements(By.cssSelector("article h2")).stream()
                .map(el -> el.getText().trim())
                .toList();
    }

    public boolean hasAllPostsLink() {
        return isElementPresent(By.linkText("Alle Beiträge anzeigen →"));
    }

    public PostListPage clickAllPosts() {
        click(By.partialLinkText("Alle Beiträge anzeigen"));
        return new PostListPage(driver, baseUrl);
    }

    public PostDetailPage clickPost(String title) {
        click(By.linkText(title));
        return new PostDetailPage(driver, baseUrl);
    }
}
