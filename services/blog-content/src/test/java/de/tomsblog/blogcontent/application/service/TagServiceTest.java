package de.tomsblog.blogcontent.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.tomsblog.blogcontent.application.port.inbound.CreateTagCommand;
import de.tomsblog.blogcontent.application.port.inbound.RenameTagCommand;
import de.tomsblog.blogcontent.application.port.outbound.TagRepository;
import de.tomsblog.blogcontent.domain.model.Tag;
import de.tomsblog.blogcontent.domain.model.TagId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    private TagService tagService;

    private final TenantId tenantId = TenantId.generate();

    @BeforeEach
    void setUp() {
        tagService = new TagService(tagRepository);
    }

    @Test
    @DisplayName("SWR-006: createTag saves tag and returns it")
    void createTag_savesAndReturns() {
        when(tagRepository.findByNameAndTenantId("Java", tenantId)).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTagCommand command = new CreateTagCommand(tenantId, "Java");
        Tag result = tagService.createTag(command);

        assertThat(result.getName()).isEqualTo("Java");
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    @DisplayName("SWR-006: createTag throws when name already exists")
    void createTag_throwsWhenDuplicate() {
        Tag existing = Tag.create(tenantId, "Java");
        when(tagRepository.findByNameAndTenantId("Java", tenantId)).thenReturn(Optional.of(existing));

        CreateTagCommand command = new CreateTagCommand(tenantId, "Java");

        assertThatThrownBy(() -> tagService.createTag(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("SWR-006: renameTag updates name and saves")
    void renameTag_updatesAndSaves() {
        Tag tag = Tag.create(tenantId, "Java");
        when(tagRepository.findByIdAndTenantId(tag.getId(), tenantId)).thenReturn(Optional.of(tag));
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        RenameTagCommand command = new RenameTagCommand(tag.getId(), tenantId, "Kotlin");
        Tag result = tagService.renameTag(command);

        assertThat(result.getName()).isEqualTo("Kotlin");
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    @DisplayName("SWR-006: renameTag throws when tag not found")
    void renameTag_throwsWhenNotFound() {
        TagId tagId = TagId.generate();
        when(tagRepository.findByIdAndTenantId(tagId, tenantId)).thenReturn(Optional.empty());

        RenameTagCommand command = new RenameTagCommand(tagId, tenantId, "NewName");

        assertThatThrownBy(() -> tagService.renameTag(command)).isInstanceOf(TagNotFoundException.class);
    }

    @Test
    @DisplayName("SWR-006: deleteTag removes tag")
    void deleteTag_removesTag() {
        Tag tag = Tag.create(tenantId, "Java");
        when(tagRepository.findByIdAndTenantId(tag.getId(), tenantId)).thenReturn(Optional.of(tag));

        tagService.deleteTag(tag.getId(), tenantId);

        verify(tagRepository).deleteByIdAndTenantId(tag.getId(), tenantId);
    }

    @Test
    @DisplayName("SWR-006: deleteTag throws when tag not found")
    void deleteTag_throwsWhenNotFound() {
        TagId tagId = TagId.generate();
        when(tagRepository.findByIdAndTenantId(tagId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.deleteTag(tagId, tenantId)).isInstanceOf(TagNotFoundException.class);
    }

    @Test
    @DisplayName("SWR-006: getTag returns tag")
    void getTag_returnsTag() {
        Tag tag = Tag.create(tenantId, "Java");
        when(tagRepository.findByIdAndTenantId(tag.getId(), tenantId)).thenReturn(Optional.of(tag));

        Tag result = tagService.getTag(tag.getId(), tenantId);

        assertThat(result).isEqualTo(tag);
    }

    @Test
    @DisplayName("SWR-006: getTag throws when not found")
    void getTag_throwsWhenNotFound() {
        TagId tagId = TagId.generate();
        when(tagRepository.findByIdAndTenantId(tagId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.getTag(tagId, tenantId)).isInstanceOf(TagNotFoundException.class);
    }

    @Test
    @DisplayName("SWR-006: listTags returns all tags for tenant")
    void listTags_returnsAll() {
        Tag tag1 = Tag.create(tenantId, "Java");
        Tag tag2 = Tag.create(tenantId, "Kotlin");
        when(tagRepository.findAllByTenantId(tenantId)).thenReturn(List.of(tag1, tag2));

        List<Tag> result = tagService.listTags(tenantId);

        assertThat(result).hasSize(2);
    }
}
