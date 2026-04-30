package de.tomsblog.blogcontent.domain.model;

import java.util.Objects;

public record Slug(String value) {

    public Slug {
        Objects.requireNonNull(value, "Slug must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Slug must not be blank");
        }
        if (!value.matches("[a-z0-9]+(-[a-z0-9]+)*")) {
            throw new IllegalArgumentException("Slug must match pattern: [a-z0-9]+(-[a-z0-9]+)*");
        }
    }

    public static Slug fromTitle(String title) {
        String slug = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return new Slug(slug);
    }
}
