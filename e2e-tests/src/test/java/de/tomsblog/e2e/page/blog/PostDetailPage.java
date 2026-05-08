package de.tomsblog.e2e.page.blog;

import de.tomsblog.e2e.page.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for the blog post detail page (GET /posts/{slug}).
 *
 * @req SWR-079
 */
public class PostDetailPage extends BasePage {

    public PostDetailPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public PostDetailPage open(String slug) {
        navigateTo("/posts/" + slug);
        waitForElement(By.tagName("h1"));
        return this;
    }

    public String getPostTitle() {
        return getText(By.cssSelector("article h1"));
    }

    public String getContent() {
        return getText(By.cssSelector("article > div"));
    }

    public String getContentHtml() {
        return waitForElement(By.cssSelector("article > div")).getDomProperty("innerHTML");
    }

    public boolean hasPublishedDate() {
        return isElementPresent(By.cssSelector("article time"));
    }

    public boolean hasPreviousPostLink() {
        return isElementPresent(By.cssSelector("nav[aria-label='Chronologische Navigation'] a[href*='/posts/']"));
    }

    public boolean hasNextPostLink() {
        return isElementPresent(By.cssSelector("nav[aria-label='Chronologische Navigation'] li:last-child a"));
    }

    public boolean hasBackToListLink() {
        return isElementPresent(By.partialLinkText("Zurück zur Übersicht"));
    }

    public PostListPage clickBackToList() {
        click(By.partialLinkText("Zurück zur Übersicht"));
        return new PostListPage(driver, baseUrl);
    }
}
