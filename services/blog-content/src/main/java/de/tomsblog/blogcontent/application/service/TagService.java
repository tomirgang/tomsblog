package de.tomsblog.blogcontent.application.service;

import de.tomsblog.blogcontent.application.port.inbound.CreateTagCommand;
import de.tomsblog.blogcontent.application.port.inbound.RenameTagCommand;
import de.tomsblog.blogcontent.application.port.inbound.TagUseCase;
import de.tomsblog.blogcontent.application.port.outbound.TagRepository;
import de.tomsblog.blogcontent.domain.model.Tag;
import de.tomsblog.blogcontent.domain.model.TagId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;

public class TagService implements TagUseCase {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Override
    public Tag createTag(CreateTagCommand command) {
        tagRepository.findByNameAndTenantId(command.name(), command.tenantId()).ifPresent(existing -> {
            throw new IllegalArgumentException("Tag with name '" + command.name() + "' already exists");
        });
        Tag tag = Tag.create(command.tenantId(), command.name());
        return tagRepository.save(tag);
    }

    @Override
    public Tag renameTag(RenameTagCommand command) {
        Tag tag = findOrThrow(command.tagId(), command.tenantId());
        tag.rename(command.newName());
        return tagRepository.save(tag);
    }

    @Override
    public void deleteTag(TagId tagId, TenantId tenantId) {
        findOrThrow(tagId, tenantId);
        tagRepository.deleteByIdAndTenantId(tagId, tenantId);
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
