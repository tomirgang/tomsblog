package de.tomsblog.e2e.page.admin;

import de.tomsblog.e2e.page.BasePage;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Page Object for the user management admin page (GET /auth/admin/users).
 *
 * @req SWR-082
 */
public class UserManagementPage extends BasePage {

    public UserManagementPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public UserManagementPage open() {
        navigateTo("/auth/admin/users");
        waitForElement(By.tagName("h2"));
        return this;
    }

    public String getHeading() {
        return getText(By.tagName("h2"));
    }

    public List<String> getUserNames() {
        return driver.findElements(By.cssSelector("tbody tr td:first-child")).stream()
                .map(WebElement::getText)
                .toList();
    }

    public boolean hasApproveButton() {
        return isElementPresent(By.xpath("//button[text()='Approve']"));
    }

    public boolean hasRejectButton() {
        return isElementPresent(By.xpath("//button[text()='Reject']"));
    }

    public void approveUser(String name) {
        WebElement row = findUserRow(name);
        if (row != null) {
            row.findElement(By.xpath(".//button[text()='Approve']")).click();
            waitForElement(By.tagName("h2"));
        }
    }

    public boolean hasSettingsLink() {
        return isElementPresent(By.linkText("Tenant-Einstellungen"));
    }

    public boolean hasEmptyMessage() {
        return driver.getPageSource().contains("Keine Benutzer in diesem Tenant.");
    }

    private WebElement findUserRow(String name) {
        List<WebElement> rows = driver.findElements(By.cssSelector("tbody tr"));
        for (WebElement row : rows) {
            String cellText = row.findElement(By.cssSelector("td:first-child")).getText();
            if (cellText.equals(name)) {
                return row;
            }
        }
        return null;
    }
}
