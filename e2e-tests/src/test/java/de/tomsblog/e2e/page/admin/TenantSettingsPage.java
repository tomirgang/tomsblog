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

    public String getHeading() {
        return getText(By.tagName("h2"));
    }
}
