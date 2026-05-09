package de.tomsblog.e2e.page.blog;

import de.tomsblog.e2e.page.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for legal pages (GET /impressum, GET /privacy).
 *
 * @req SWR-079
 */
public class LegalPage extends BasePage {

    public LegalPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public LegalPage openImpressum() {
        navigateTo("/impressum");
        waitForElement(By.tagName("h2"));
        return this;
    }

    public LegalPage openPrivacy() {
        navigateTo("/privacy");
        waitForElement(By.tagName("h2"));
        return this;
    }

    public String getHeading() {
        return getText(By.tagName("h2"));
    }

    public String getContent() {
        return getText(By.cssSelector("section"));
    }
}
