package de.tomsblog.e2e.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * JUnit 5 extension that captures a browser screenshot on test failure. Screenshots are saved to
 * {@code target/screenshots/}.
 *
 * @req SWR-078
 */
public class ScreenshotOnFailureExtension implements TestWatcher {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        context.getTestInstance().ifPresent(instance -> {
            if (instance instanceof WebDriverProvider provider) {
                WebDriver driver = provider.getWebDriver();
                if (driver instanceof TakesScreenshot screenshotter) {
                    takeScreenshot(screenshotter, context);
                }
            }
        });
    }

    private void takeScreenshot(TakesScreenshot screenshotter, ExtensionContext context) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String testClass = context.getRequiredTestClass().getSimpleName();
        String testMethod = context.getRequiredTestMethod().getName();
        String filename = String.format("%s_%s_%s.png", timestamp, testClass, testMethod);

        try {
            Path screenshotDir = Path.of("target", "screenshots");
            Files.createDirectories(screenshotDir);
            File screenshot = screenshotter.getScreenshotAs(OutputType.FILE);
            Files.copy(screenshot.toPath(), screenshotDir.resolve(filename));
        } catch (IOException e) {
            // Best effort: log but don't fail the test because of screenshot issues
            System.err.println("Failed to save screenshot: " + e.getMessage());
        }
    }
}
