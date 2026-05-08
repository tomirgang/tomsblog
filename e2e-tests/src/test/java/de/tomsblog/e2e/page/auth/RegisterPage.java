package de.tomsblog.e2e.page.auth;

import de.tomsblog.e2e.page.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for the user registration page (GET /auth/register).
 *
 * @req SWR-081
 */
public class RegisterPage extends BasePage {

    public RegisterPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public RegisterPage open() {
        navigateTo("/auth/register");
        waitForElement(By.tagName("h1"));
        return this;
    }

    public String getHeading() {
        return getText(By.tagName("h1"));
    }

    public RegisterPage fillForm(String username, String email, String displayName, String password) {
        type(By.id("username"), username);
        type(By.id("email"), email);
        type(By.id("displayName"), displayName);
        type(By.id("password"), password);
        type(By.id("passwordConfirm"), password);
        return this;
    }

    public void submit() {
        click(By.cssSelector("button[type='submit']"));
    }

    public boolean hasErrorMessage() {
        return isElementPresent(By.cssSelector("[role='alert']"));
    }

    public String getErrorMessage() {
        return getText(By.cssSelector("[role='alert']"));
    }

    public boolean hasLoginLink() {
        return isElementPresent(By.linkText("Anmelden"));
    }
}
