package de.tomsblog.e2e.page.blog;

import de.tomsblog.e2e.page.BasePage;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Page Object for the blog post list page (GET /posts).
 *
 * @req SWR-079
 */
public class PostListPage extends BasePage {

    public PostListPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public PostListPage open() {
        navigateTo("/posts");
        waitForElement(By.tagName("h1"));
        return this;
    }

    public String getHeading() {
        return getText(By.tagName("h1"));
    }

    public PostListPage search(String query) {
        type(By.cssSelector("input[name='q']"), query);
        click(By.cssSelector("button[type='submit']"));
        waitForElement(By.tagName("h1"));
        return this;
    }

    public boolean hasSearchResults() {
        return isElementPresent(By.className("search-info"));
    }

    public int getSearchResultCount() {
        String text = getText(By.className("search-info"));
        return Integer.parseInt(text.split(" ")[0]);
    }

    public List<String> getPostTitles() {
        return driver.findElements(By.cssSelector("article h2")).stream()
                .map(el -> el.getText().trim())
                .toList();
    }

    public PostDetailPage clickPost(String title) {
        click(By.linkText(title));
        return new PostDetailPage(driver, baseUrl);
    }

    public boolean hasNewPostButton() {
        return isElementPresent(By.linkText("Neuen Post erstellen"));
    }

    public PostFormPage clickNewPost() {
        click(By.linkText("Neuen Post erstellen"));
        return new PostFormPage(driver, baseUrl);
    }

    public boolean hasEmptyMessage() {
        return driver.getPageSource().contains("Noch keine Beiträge vorhanden.");
    }

    /** @req SWR-086 */
    public boolean hasTagLabels() {
        return isElementPresent(By.cssSelector(".tag-label"));
    }

    /** @req SWR-086 */
    public List<String> getTagLabelsForPost(String title) {
        var articles = driver.findElements(By.cssSelector("article"));
        for (var article : articles) {
            String heading = article.findElement(By.tagName("h2")).getText().trim();
            if (heading.equals(title)) {
                return article.findElements(By.cssSelector(".tag-label")).stream()
                        .map(WebElement::getText)
                        .toList();
            }
        }
        return List.of();
    }
}
