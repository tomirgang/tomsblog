package de.tomsblog.blogcontent.domain.model;

import de.tomsblog.shared.tenant.TenantId;
import java.util.Objects;

/**
 * Tag entity with identity, scoped to a tenant.
 */
public class Tag {

    private final TagId id;
    private final TenantId tenantId;
    private String name;
    private Slug slug;

    private Tag(TagId id, TenantId tenantId, String name, Slug slug) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.name = Objects.requireNonNull(name);
        this.slug = Objects.requireNonNull(slug);
    }

    public static Tag create(TenantId tenantId, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tag name must not be blank");
        }
        TagId id = TagId.generate();
        Slug slug = Slug.fromTitle(name);
        return new Tag(id, tenantId, name, slug);
    }

    public static Tag reconstitute(TagId id, TenantId tenantId, String name, Slug slug) {
        return new Tag(id, tenantId, name, slug);
    }

    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Tag name must not be blank");
        }
        this.name = newName;
        this.slug = Slug.fromTitle(newName);
    }

    public TagId getId() {
        return id;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public Slug getSlug() {
        return slug;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return Objects.equals(id, tag.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
