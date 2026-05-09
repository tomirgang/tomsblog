package de.tomsblog.e2e.page.auth;

import de.tomsblog.e2e.page.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for the user login page (GET /auth/login).
 *
 * @req SWR-081
 */
public class LoginPage extends BasePage {

    public LoginPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public LoginPage open() {
        navigateTo("/auth/login");
        waitForElement(By.tagName("h1"));
        return this;
    }

    public String getHeading() {
        return getText(By.tagName("h1"));
    }

    public void login(String username, String password) {
        // In BOTH mode, the internal login form is inside a collapsed <details> element
        var summaries = driver.findElements(By.cssSelector("details summary"));
        if (!summaries.isEmpty()) {
            summaries.get(0).click();
        }
        type(By.id("username"), username);
        type(By.id("password"), password);
        click(By.cssSelector("button[type='submit']"));
    }

    public boolean hasErrorMessage() {
        return isElementPresent(By.cssSelector("[role='alert']"));
    }

    public String getErrorMessage() {
        return getText(By.cssSelector("[role='alert']"));
    }

    public boolean hasOidcButton() {
        return isElementPresent(By.partialLinkText("Authentik"));
    }

    public boolean hasInternalLoginForm() {
        return isElementPresent(By.id("username"));
    }

    public boolean hasRegisterLink() {
        return isElementPresent(By.linkText("Registrieren"));
    }

    public RegisterPage clickRegister() {
        click(By.linkText("Registrieren"));
        return new RegisterPage(driver, baseUrl);
    }
}
