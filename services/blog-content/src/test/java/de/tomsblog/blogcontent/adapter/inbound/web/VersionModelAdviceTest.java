package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;

class VersionModelAdviceTest {

    @Test
    @DisplayName("SWR-034: Returns version from BuildProperties when present")
    void returnsBuildPropertiesVersion() {
        java.util.Properties props = new java.util.Properties();
        props.setProperty("version", "1.2.3");
        BuildProperties buildProperties = new BuildProperties(props);

        VersionModelAdvice advice = new VersionModelAdvice("unknown", Optional.of(buildProperties));

        assertThat(advice.appVersion()).isEqualTo("1.2.3");
    }

    @Test
    @DisplayName("SWR-034: Falls back to property value when BuildProperties absent")
    void fallsBackToPropertyValue() {
        VersionModelAdvice advice = new VersionModelAdvice("0.5.3-SNAPSHOT", Optional.empty());

        assertThat(advice.appVersion()).isEqualTo("0.5.3-SNAPSHOT");
    }
}
