package de.tomsblog.e2e.page.admin;

import de.tomsblog.e2e.page.BasePage;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Page Object for the tenant list (SuperAdmin) page (GET /tenant/admin/tenants).
 *
 * @req SWR-083
 */
public class TenantListPage extends BasePage {

    public TenantListPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public TenantListPage open() {
        navigateTo("/tenant/admin/tenants");
        waitForElement(By.tagName("h2"));
        return this;
    }

    public String getHeading() {
        return getText(By.tagName("h2"));
    }

    public List<String> getTenantSlugs() {
        return driver.findElements(By.cssSelector("tbody tr td:first-child")).stream()
                .map(WebElement::getText)
                .toList();
    }

    public TenantListPage createTenant(String slug, String displayName) {
        type(By.id("slug"), slug);
        type(By.id("displayName"), displayName);
        click(By.cssSelector("main form button[type='submit']"));
        waitForElement(By.tagName("h2"));
        return this;
    }

    public boolean hasSettingsLink() {
        return isElementPresent(By.linkText("Tenant-Einstellungen"));
    }

    public GlobalSettingsPage clickSettings() {
        click(By.linkText("Tenant-Einstellungen"));
        return new GlobalSettingsPage(driver, baseUrl);
    }
}
