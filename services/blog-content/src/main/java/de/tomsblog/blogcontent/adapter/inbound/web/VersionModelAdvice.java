package de.tomsblog.blogcontent.adapter.inbound.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Makes the application version available as a model attribute on every page.
 *
 * @req SWR-034
 */
@ControllerAdvice
public class VersionModelAdvice {

    private final String appVersion;

    public VersionModelAdvice(
            @Value("${spring.application.version:unknown}") String fallbackVersion,
            java.util.Optional<BuildProperties> buildProperties) {
        this.appVersion = buildProperties.map(BuildProperties::getVersion).orElse(fallbackVersion);
    }

    @ModelAttribute("appVersion")
    public String appVersion() {
        return appVersion;
    }
}
