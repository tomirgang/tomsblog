package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @req SWR-026
 */
class WebExceptionHandlerTest {

    private final WebExceptionHandler handler = new WebExceptionHandler();

    @Test
    @DisplayName("SWR-026: handlePostNotFound returns 404 view")
    void handlePostNotFound_returns404View() {
        String view = handler.handlePostNotFound();
        assertThat(view).isEqualTo("error/404");
    }

    @Test
    @DisplayName("SWR-026: handleUnexpected returns 500 view")
    void handleUnexpected_returns500View() {
        String view = handler.handleUnexpected();
        assertThat(view).isEqualTo("error/500");
    }
}
