package de.tomsblog.e2e.page.auth;

import de.tomsblog.e2e.page.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for the admin break-glass login page (GET /auth/admin/login or GET
 * /tenant/admin/login).
 *
 * @req SWR-081
 * @req SWR-083
 */
public class AdminLoginPage extends BasePage {

    private final String loginPath;

    public AdminLoginPage(WebDriver driver, String baseUrl, String loginPath) {
        super(driver, baseUrl);
        this.loginPath = loginPath;
    }

    public AdminLoginPage open() {
        navigateTo(loginPath);
        waitForElement(By.tagName("h1"));
        return this;
    }

    public String getHeading() {
        return getText(By.tagName("h1"));
    }

    public void login(String username, String password) {
        type(By.id("username"), username);
        type(By.id("password"), password);
        click(By.cssSelector("button[type='submit']"));
    }

    public boolean hasErrorMessage() {
        return isElementPresent(By.cssSelector("[role='alert']"));
    }
}
