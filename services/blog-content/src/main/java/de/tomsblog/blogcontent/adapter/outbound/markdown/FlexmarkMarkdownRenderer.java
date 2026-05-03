package de.tomsblog.blogcontent.adapter.outbound.markdown;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import de.tomsblog.blogcontent.application.port.outbound.MarkdownRenderer;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

/**
 * Flexmark-based Markdown renderer with OWASP HTML sanitization.
 *
 * @req SWR-035
 */
@Component
public class FlexmarkMarkdownRenderer implements MarkdownRenderer {

    private static final PolicyFactory SANITIZE_POLICY = new HtmlPolicyBuilder()
            .allowCommonBlockElements()
            .allowCommonInlineFormattingElements()
            .allowElements(
                    "a",
                    "img",
                    "table",
                    "thead",
                    "tbody",
                    "tr",
                    "th",
                    "td",
                    "pre",
                    "code",
                    "blockquote",
                    "hr",
                    "br",
                    "dl",
                    "dt",
                    "dd",
                    "figure",
                    "figcaption",
                    "details",
                    "summary")
            .allowAttributes("href")
            .onElements("a")
            .allowAttributes("src", "alt", "title", "width", "height")
            .onElements("img")
            .allowAttributes("class")
            .onElements("pre", "code", "div", "span")
            .allowAttributes("target", "rel")
            .onElements("a")
            .allowAttributes("id")
            .globally()
            .allowUrlProtocols("http", "https", "mailto")
            .requireRelNofollowOnLinks()
            .toFactory();

    private final Parser parser;
    private final HtmlRenderer renderer;

    public FlexmarkMarkdownRenderer() {
        MutableDataSet options = new MutableDataSet();
        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }

    @Override
    public String renderToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node document = parser.parse(markdown);
        String html = renderer.render(document);
        return SANITIZE_POLICY.sanitize(html);
    }
}
