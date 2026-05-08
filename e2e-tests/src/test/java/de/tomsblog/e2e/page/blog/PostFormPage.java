package de.tomsblog.e2e.page.blog;

import de.tomsblog.e2e.page.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

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
}
