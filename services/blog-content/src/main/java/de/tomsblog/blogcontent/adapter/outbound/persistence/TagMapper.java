package de.tomsblog.blogcontent.adapter.outbound.persistence;

import de.tomsblog.blogcontent.domain.model.Slug;
import de.tomsblog.blogcontent.domain.model.Tag;
import de.tomsblog.blogcontent.domain.model.TagId;
import de.tomsblog.shared.tenant.TenantId;

public class TagMapper {

    private TagMapper() {}

    public static TagJpaEntity toEntity(Tag tag) {
        TagJpaEntity entity = new TagJpaEntity();
        entity.setId(tag.getId().value());
        entity.setTenantId(tag.getTenantId().value());
        entity.setName(tag.getName());
        entity.setSlug(tag.getSlug().value());
        return entity;
    }

    public static Tag toDomain(TagJpaEntity entity) {
        return Tag.reconstitute(
                TagId.of(entity.getId()),
                TenantId.of(entity.getTenantId()),
                entity.getName(),
                new Slug(entity.getSlug()));
    }
}
