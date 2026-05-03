package de.tomsblog.blogcontent.application.port.outbound;

/**
 * Outbound port for rendering Markdown content to HTML.
 *
 * @req SWR-035
 */
public interface MarkdownRenderer {

    String renderToHtml(String markdown);
}
