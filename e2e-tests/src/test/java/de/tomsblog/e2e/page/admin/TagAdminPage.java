package de.tomsblog.e2e.page.admin;

import de.tomsblog.e2e.page.BasePage;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Page Object for the tag management page (GET /admin/tags).
 *
 * @req SWR-084
 */
public class TagAdminPage extends BasePage {

    public TagAdminPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public TagAdminPage open() {
        navigateTo("/admin/tags");
        waitForElement(By.tagName("h1"));
        return this;
    }

    public String getHeading() {
        return getText(By.tagName("h1"));
    }

    public boolean hasEmptyMessage() {
        return driver.getPageSource().contains("Noch keine Tags vorhanden.");
    }

    public TagAdminPage createTag(String name) {
        type(By.cssSelector("input[name='name'][placeholder='Tag-Name']"), name);
        click(By.cssSelector("form[action$='/admin/tags'] button[type='submit']"));
        waitForElement(By.tagName("h1"));
        return this;
    }

    public List<String> getTagNames() {
        return driver.findElements(By.cssSelector("table tbody tr td:first-child")).stream()
                .map(WebElement::getText)
                .toList();
    }

    public List<String> getTagSlugs() {
        return driver.findElements(By.cssSelector("table tbody tr td:nth-child(2)")).stream()
                .map(WebElement::getText)
                .toList();
    }

    public TagAdminPage renameTag(String oldName, String newName) {
        WebElement row = findTagRow(oldName);
        WebElement nameInput = row.findElement(By.cssSelector("input[name='name']"));
        nameInput.clear();
        nameInput.sendKeys(newName);
        row.findElement(By.xpath(".//button[text()='Umbenennen']")).click();
        waitForElement(By.tagName("h1"));
        return this;
    }

    public TagAdminPage deleteTag(String name) {
        WebElement row = findTagRow(name);
        WebElement deleteButton = row.findElement(By.xpath(".//button[text()='Löschen']"));
        // Submit the parent form directly via JS to bypass onclick confirm dialog
        WebElement form = deleteButton.findElement(By.xpath("./.."));
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].submit()", form);
        waitForElement(By.tagName("h1"));
        return this;
    }

    public boolean hasTagTable() {
        return isElementPresent(By.cssSelector("table"));
    }

    private WebElement findTagRow(String tagName) {
        return driver.findElements(By.cssSelector("table tbody tr")).stream()
                .filter(row -> row.findElement(By.cssSelector("td:first-child"))
                        .getText()
                        .equals(tagName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Tag row not found: " + tagName));
    }
}
