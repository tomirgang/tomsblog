package de.tomsblog.e2e.page.admin;

import de.tomsblog.e2e.page.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for the global tenant settings (SuperAdmin) page (GET /tenant/admin/settings).
 *
 * @req SWR-083
 */
public class GlobalSettingsPage extends BasePage {

    public GlobalSettingsPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public GlobalSettingsPage open() {
        navigateTo("/tenant/admin/settings?tab=general");
        waitForElement(By.tagName("h2"));
        return this;
    }

    public String getHeading() {
        return getText(By.tagName("h2"));
    }

    public boolean hasGeneralTab() {
        return isElementPresent(By.linkText("Allgemein"));
    }

    public boolean hasOidcTab() {
        return isElementPresent(By.linkText("OIDC"));
    }

    public boolean hasLegalTab() {
        return isElementPresent(By.partialLinkText("Impressum"));
    }

    public GlobalSettingsPage switchToTab(String tabName) {
        click(By.linkText(tabName));
        waitForElement(By.tagName("h2"));
        return this;
    }

    public GlobalSettingsPage fillDisplayName(String name) {
        type(By.id("displayName"), name);
        return this;
    }

    public GlobalSettingsPage fillTagline(String tagline) {
        type(By.id("tagline"), tagline);
        return this;
    }

    public void submitGeneralForm() {
        click(By.cssSelector(".tab-content.active button[type='submit']"));
    }

    public boolean hasTenantListLink() {
        return isElementPresent(By.linkText("Tenant-Verwaltung"));
    }
}
