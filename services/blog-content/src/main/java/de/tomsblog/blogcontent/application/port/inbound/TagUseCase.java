package de.tomsblog.blogcontent.application.port.inbound;

import de.tomsblog.blogcontent.domain.model.Tag;
import de.tomsblog.blogcontent.domain.model.TagId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;

/**
 * Inbound port for tag management use cases.
 *
 * @req SWR-020
 */
public interface TagUseCase {

    Tag createTag(CreateTagCommand command);

    Tag renameTag(RenameTagCommand command);

    void deleteTag(TagId tagId, TenantId tenantId);

    Tag getTag(TagId tagId, TenantId tenantId);

    List<Tag> listTags(TenantId tenantId);
}
