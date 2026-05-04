package de.tomsblog.blogcontent.application.service;

import de.tomsblog.blogcontent.application.port.inbound.CreateTagCommand;
import de.tomsblog.blogcontent.application.port.inbound.RenameTagCommand;
import de.tomsblog.blogcontent.application.port.inbound.TagUseCase;
import de.tomsblog.blogcontent.application.port.outbound.TagRepository;
import de.tomsblog.blogcontent.domain.model.Tag;
import de.tomsblog.blogcontent.domain.model.TagId;
import de.tomsblog.shared.audit.AuditLogEntry;
import de.tomsblog.shared.audit.AuditLogger;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;

/**
 * Application service orchestrating tag use cases.
 *
 * @req SWR-020
 * @req SWR-056
 */
public class TagService implements TagUseCase {

    private final TagRepository tagRepository;
    private final AuditLogger auditLogger;

    public TagService(TagRepository tagRepository, AuditLogger auditLogger) {
        this.tagRepository = tagRepository;
        this.auditLogger = auditLogger;
    }

    @Override
    public Tag createTag(CreateTagCommand command) {
        tagRepository.findByNameAndTenantId(command.name(), command.tenantId()).ifPresent(existing -> {
            throw new IllegalArgumentException("Tag with name '" + command.name() + "' already exists");
        });
        Tag tag = Tag.create(command.tenantId(), command.name());
        Tag saved = tagRepository.save(tag);
        auditLogger.log(AuditLogEntry.create(
                command.tenantId().toString(),
                "system",
                "TAG_CREATED",
                "Tag",
                saved.getId().asString(),
                command.name()));
        return saved;
    }

    @Override
    public Tag renameTag(RenameTagCommand command) {
        Tag tag = findOrThrow(command.tagId(), command.tenantId());
        tag.rename(command.newName());
        Tag saved = tagRepository.save(tag);
        auditLogger.log(AuditLogEntry.create(
                command.tenantId().toString(),
                "system",
                "TAG_RENAMED",
                "Tag",
                command.tagId().asString(),
                command.newName()));
        return saved;
    }

    @Override
    public void deleteTag(TagId tagId, TenantId tenantId) {
        findOrThrow(tagId, tenantId);
        tagRepository.deleteByIdAndTenantId(tagId, tenantId);
        auditLogger.log(AuditLogEntry.create(tenantId.toString(), "system", "TAG_DELETED", "Tag", tagId.asString()));
    }

    @Override
    public Tag getTag(TagId tagId, TenantId tenantId) {
        return findOrThrow(tagId, tenantId);
    }

    @Override
    public List<Tag> listTags(TenantId tenantId) {
        return tagRepository.findAllByTenantId(tenantId);
    }

    private Tag findOrThrow(TagId tagId, TenantId tenantId) {
        return tagRepository.findByIdAndTenantId(tagId, tenantId).orElseThrow(() -> new TagNotFoundException(tagId));
    }
}
