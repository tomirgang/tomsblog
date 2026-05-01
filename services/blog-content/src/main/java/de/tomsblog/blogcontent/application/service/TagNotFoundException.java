package de.tomsblog.blogcontent.application.service;

import de.tomsblog.blogcontent.domain.model.TagId;

public class TagNotFoundException extends RuntimeException {

    private final TagId tagId;

    public TagNotFoundException(TagId tagId) {
        super("Tag not found: " + tagId.value());
        this.tagId = tagId;
    }

    public TagId getTagId() {
        return tagId;
    }
}
