package de.tomsblog.blogcontent.domain.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * A reference source for a blog post (URL + title).
 *
 * @req SWR-012
 */
public record Source(String url, String title) {

    public Source {
        Objects.requireNonNull(url, "Source URL must not be null");
        if (url.isBlank()) {
            throw new IllegalArgumentException("Source URL must not be blank");
        }
        if (!url.matches("^https?://.*")) {
            throw new IllegalArgumentException("Source URL must use http or https protocol");
        }
        try {
            new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Source URL is not a valid URI", e);
        }
        Objects.requireNonNull(title, "Source title must not be null");
        if (title.isBlank()) {
            throw new IllegalArgumentException("Source title must not be blank");
        }
    }
}
