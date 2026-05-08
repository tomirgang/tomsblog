package de.tomsblog.e2e.page.admin;

import de.tomsblog.e2e.page.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for the tenant settings admin page (GET /auth/admin/settings).
 *
 * @req SWR-082
 */
public class TenantSettingsPage extends BasePage {

    public TenantSettingsPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public TenantSettingsPage open() {
        navigateTo("/auth/admin/settings");
        waitForElement(By.tagName("h2"));
        return this;
    }

    public TenantSettingsPage openTab(String tab) {
        navigateTo("/auth/admin/settings?tab=" + tab);
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

    public TenantSettingsPage fillDisplayName(String name) {
        type(By.id("displayName"), name);
        return this;
    }

    public TenantSettingsPage fillTagline(String tagline) {
        type(By.id("tagline"), tagline);
        return this;
    }

    public TenantSettingsPage fillImpressum(String content) {
        type(By.id("impressumContent"), content);
        return this;
    }

    public TenantSettingsPage fillPrivacyPolicy(String content) {
        type(By.id("privacyPolicyContent"), content);
        return this;
    }

    public void submitForm() {
        click(By.cssSelector("button[type='submit']"));
        waitForElement(By.tagName("h2"));
    }

    public boolean hasSubmitButton() {
        return isElementPresent(By.cssSelector("button[type='submit']"));
    }
}
