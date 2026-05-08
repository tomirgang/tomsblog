package de.tomsblog.e2e.page.blog;

import de.tomsblog.e2e.page.BasePage;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Page Object for the blog post creation/editing form (GET /posts/new, GET /posts/{id}/edit).
 *
 * @req SWR-080
 */
public class PostFormPage extends BasePage {

    public PostFormPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public PostFormPage open() {
        navigateTo("/posts/new");
        waitForElement(By.id("title"));
        return this;
    }

    public String getHeading() {
        return getText(By.tagName("h1"));
    }

    public PostFormPage fillTitle(String title) {
        type(By.id("title"), title);
        return this;
    }

    public PostFormPage fillContent(String content) {
        type(By.id("content"), content);
        return this;
    }

    public PostFormPage selectContentType(String contentType) {
        selectByValue(By.id("contentType"), contentType);
        return this;
    }

    public PostFormPage selectLocale(String locale) {
        selectByValue(By.id("locale"), locale);
        return this;
    }

    public void submit() {
        click(By.cssSelector("button[type='submit']"));
    }

    public boolean hasPreviewButton() {
        return isElementPresent(By.linkText("Vorschau"));
    }

    public boolean hasCancelButton() {
        return isElementPresent(By.linkText("Abbrechen"));
    }

    public boolean hasValidationError() {
        return isElementPresent(By.className("error"));
    }

    public String getValidationError() {
        return getText(By.className("error"));
    }

    /** @req SWR-085 */
    public boolean hasTagSection() {
        return driver.getPageSource().contains("Tags");
    }

    /** @req SWR-085 */
    public List<String> getAvailableTagNames() {
        return driver.findElements(By.cssSelector("input[name='tagIds']")).stream()
                .map(el -> el.findElement(By.xpath("./following-sibling::span")).getText())
                .toList();
    }

    /** @req SWR-085 */
    public PostFormPage selectTag(String tagName) {
        List<WebElement> checkboxes = driver.findElements(By.cssSelector("input[name='tagIds']"));
        for (WebElement cb : checkboxes) {
            String label = cb.findElement(By.xpath("./following-sibling::span")).getText();
            if (label.equals(tagName) && !cb.isSelected()) {
                cb.click();
                break;
            }
        }
        return this;
    }

    /** @req SWR-085 */
    public PostFormPage fillNewTagName(String name) {
        type(By.id("newTagName"), name);
        return this;
    }

    /** @req SWR-085 */
    public boolean hasNewTagNameField() {
        return isElementPresent(By.id("newTagName"));
    }
}
