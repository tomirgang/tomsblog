package de.tomsblog.blogcontent.adapter.inbound.rest;

import de.tomsblog.blogcontent.domain.model.Tag;
import java.util.UUID;

public record TagResponse(UUID id, UUID tenantId, String name, String slug) {

    public static TagResponse from(Tag tag) {
        return new TagResponse(
                tag.getId().value(),
                tag.getTenantId().value(),
                tag.getName(),
                tag.getSlug().value());
    }
}
