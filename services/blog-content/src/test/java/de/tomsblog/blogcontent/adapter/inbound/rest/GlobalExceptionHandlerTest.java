package de.tomsblog.blogcontent.adapter.inbound.rest;

import static org.assertj.core.api.Assertions.assertThat;

import de.tomsblog.blogcontent.application.service.PostNotFoundException;
import de.tomsblog.blogcontent.application.service.TagNotFoundException;
import de.tomsblog.blogcontent.application.service.TranslationNotFoundException;
import de.tomsblog.blogcontent.domain.model.PostId;
import de.tomsblog.blogcontent.domain.model.TagId;
import de.tomsblog.blogcontent.domain.model.TranslationId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * @req SWR-015
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("SWR-015: handlePostNotFound returns 404 ProblemDetail")
    void handlePostNotFound_returns404() {
        PostId postId = PostId.generate();
        var ex = new PostNotFoundException(postId);

        ProblemDetail result = handler.handlePostNotFound(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getTitle()).isEqualTo("Post Not Found");
    }

    @Test
    @DisplayName("SWR-015: handleTagNotFound returns 404 ProblemDetail")
    void handleTagNotFound_returns404() {
        TagId tagId = TagId.generate();
        var ex = new TagNotFoundException(tagId);

        ProblemDetail result = handler.handleTagNotFound(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getTitle()).isEqualTo("Tag Not Found");
    }

    @Test
    @DisplayName("SWR-015: handleTranslationNotFound returns 404 ProblemDetail")
    void handleTranslationNotFound_returns404() {
        TranslationId translationId = TranslationId.generate();
        var ex = new TranslationNotFoundException(translationId);

        ProblemDetail result = handler.handleTranslationNotFound(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getTitle()).isEqualTo("Translation Not Found");
    }

    @Test
    @DisplayName("SWR-015: handleMessageNotReadable returns 400 ProblemDetail")
    void handleMessageNotReadable_returns400() {
        var ex = new HttpMessageNotReadableException("Bad body", (HttpInputMessage) null);

        ProblemDetail result = handler.handleMessageNotReadable(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Invalid Request");
        assertThat(result.getDetail()).isEqualTo("Malformed request body");
    }

    @Test
    @DisplayName("SWR-015: handleTypeMismatch returns 400 ProblemDetail")
    void handleTypeMismatch_returns400() {
        var ex = new MethodArgumentTypeMismatchException("abc", UUID.class, "id", null, null);

        ProblemDetail result = handler.handleTypeMismatch(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Invalid Request");
        assertThat(result.getDetail()).isEqualTo("Invalid parameter type");
    }

    @Test
    @DisplayName("SWR-015: handleNoResourceFound returns 404 ProblemDetail")
    void handleNoResourceFound_returns404() throws NoResourceFoundException {
        var ex = new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "/api/unknown");

        ProblemDetail result = handler.handleNoResourceFound(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getTitle()).isEqualTo("Not Found");
    }

    @Test
    @DisplayName("SWR-015: handleIllegalArgument returns 400 ProblemDetail")
    void handleIllegalArgument_returns400() {
        var ex = new IllegalArgumentException("bad argument");

        ProblemDetail result = handler.handleIllegalArgument(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Invalid Request");
        assertThat(result.getDetail()).isEqualTo("bad argument");
    }

    @Test
    @DisplayName("SWR-015: handleIllegalState returns 409 ProblemDetail")
    void handleIllegalState_returns409() {
        var ex = new IllegalStateException("conflict");

        ProblemDetail result = handler.handleIllegalState(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getTitle()).isEqualTo("Conflict");
        assertThat(result.getDetail()).isEqualTo("conflict");
    }

    @Test
    @DisplayName("SWR-015: handleUnexpectedException returns 500 ProblemDetail")
    void handleUnexpectedException_returns500() {
        var ex = new RuntimeException("unexpected");

        ProblemDetail result = handler.handleUnexpectedException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getTitle()).isEqualTo("Internal Server Error");
        assertThat(result.getDetail()).isEqualTo("An unexpected error occurred");
    }
}
