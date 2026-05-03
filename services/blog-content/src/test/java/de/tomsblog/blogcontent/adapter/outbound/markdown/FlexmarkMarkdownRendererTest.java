package de.tomsblog.blogcontent.adapter.outbound.markdown;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlexmarkMarkdownRendererTest {

    private final FlexmarkMarkdownRenderer renderer = new FlexmarkMarkdownRenderer();

    @Test
    @DisplayName("SWR-035: Renders Markdown to HTML")
    void rendersMarkdownToHtml() {
        String html = renderer.renderToHtml("# Hello\n\nWorld");
        assertThat(html).contains("<h1>Hello</h1>");
        assertThat(html).contains("<p>World</p>");
    }

    @Test
    @DisplayName("SWR-035: Returns empty string for null input")
    void returnsEmptyForNull() {
        assertThat(renderer.renderToHtml(null)).isEmpty();
    }

    @Test
    @DisplayName("SWR-035: Returns empty string for blank input")
    void returnsEmptyForBlank() {
        assertThat(renderer.renderToHtml("   ")).isEmpty();
    }

    @Test
    @DisplayName("SWR-035: Renders code blocks with language info")
    void rendersCodeBlocks() {
        String markdown = "```java\nSystem.out.println();\n```";
        String html = renderer.renderToHtml(markdown);
        assertThat(html).contains("<code");
        assertThat(html).contains("System.out.println()");
    }
}
