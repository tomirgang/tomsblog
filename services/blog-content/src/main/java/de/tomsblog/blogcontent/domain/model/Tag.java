package de.tomsblog.blogcontent.domain.model;

import java.util.Objects;

public record Tag(String name) {

    public Tag {
        Objects.requireNonNull(name, "Tag name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Tag name must not be blank");
        }
    }
}
