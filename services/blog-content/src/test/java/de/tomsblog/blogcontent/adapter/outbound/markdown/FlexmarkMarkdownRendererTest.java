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

    @Test
    @DisplayName("SWR-035: Sanitizes script tags from inline HTML")
    void sanitizesScriptTags() {
        String markdown = "Hello <script>alert('xss')</script> World";
        String html = renderer.renderToHtml(markdown);
        assertThat(html).doesNotContain("<script>");
        assertThat(html).doesNotContain("alert");
    }

    @Test
    @DisplayName("SWR-035: Sanitizes javascript: URLs from links")
    void sanitizesJavascriptUrls() {
        String markdown = "[click](javascript:alert('xss'))";
        String html = renderer.renderToHtml(markdown);
        assertThat(html).doesNotContain("javascript:");
    }

    @Test
    @DisplayName("SWR-035: Allows safe HTML elements from Markdown")
    void allowsSafeHtmlElements() {
        String markdown = "**bold** and *italic* and [link](https://example.com)";
        String html = renderer.renderToHtml(markdown);
        assertThat(html).contains("<strong>bold</strong>");
        assertThat(html).contains("<em>italic</em>");
        assertThat(html).contains("href=\"https://example.com\"");
    }

    @Test
    @DisplayName("SWR-035: Sanitizes event handler attributes from raw HTML blocks")
    void sanitizesEventHandlers() {
        // Raw HTML block (blank lines around make it a block element for Flexmark)
        String markdown = "\n<div onmouseover=\"alert('xss')\">hover me</div>\n";
        String html = renderer.renderToHtml(markdown);
        assertThat(html).doesNotContain("onmouseover");
        assertThat(html).contains("hover me");
    }

    @Test
    @DisplayName("SWR-035: Renders Markdown tables as HTML tables")
    void rendersMarkdownTables() {
        String markdown = "| Header 1 | Header 2 |\n| --- | --- |\n| Cell 1 | Cell 2 |";
        String html = renderer.renderToHtml(markdown);
        assertThat(html).contains("<table>");
        assertThat(html).contains("<thead>");
        assertThat(html).contains("<th>Header 1</th>");
        assertThat(html).contains("<td>Cell 1</td>");
    }
}
