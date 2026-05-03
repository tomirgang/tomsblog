package de.tomsblog.blogcontent.adapter.outbound.markdown;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import de.tomsblog.blogcontent.application.port.outbound.MarkdownRenderer;
import org.springframework.stereotype.Component;

/**
 * Flexmark-based Markdown renderer.
 *
 * @req SWR-035
 */
@Component
public class FlexmarkMarkdownRenderer implements MarkdownRenderer {

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
        return renderer.render(document);
    }
}
