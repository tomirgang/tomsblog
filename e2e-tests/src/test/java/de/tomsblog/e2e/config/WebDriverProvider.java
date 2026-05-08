package de.tomsblog.e2e.config;

import org.openqa.selenium.WebDriver;

/**
 * Interface for test classes that provide a {@link WebDriver} instance. Used by {@link
 * ScreenshotOnFailureExtension} to capture screenshots on failure.
 *
 * @req SWR-078
 */
public interface WebDriverProvider {

    WebDriver getWebDriver();
}
