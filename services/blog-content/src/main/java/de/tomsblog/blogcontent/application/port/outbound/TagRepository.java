package de.tomsblog.blogcontent.application.port.outbound;

import de.tomsblog.blogcontent.domain.model.Tag;
import de.tomsblog.blogcontent.domain.model.TagId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.Optional;

public interface TagRepository {

    Tag save(Tag tag);

    Optional<Tag> findByIdAndTenantId(TagId id, TenantId tenantId);

    List<Tag> findAllByTenantId(TenantId tenantId);

    Optional<Tag> findByNameAndTenantId(String name, TenantId tenantId);

    void deleteByIdAndTenantId(TagId id, TenantId tenantId);
}
