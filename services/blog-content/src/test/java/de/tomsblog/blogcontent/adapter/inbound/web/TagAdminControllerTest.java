package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import de.tomsblog.blogcontent.application.port.inbound.CreateTagCommand;
import de.tomsblog.blogcontent.application.port.inbound.RenameTagCommand;
import de.tomsblog.blogcontent.application.port.inbound.TagUseCase;
import de.tomsblog.blogcontent.domain.model.Tag;
import de.tomsblog.blogcontent.domain.model.TagId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TagAdminController.class)
@Import(SecurityConfiguration.class)
class TagAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagUseCase tagUseCase;

    @MockitoBean
    private UserManagementClient userManagementClient;

    private final UUID tenantId = UUID.randomUUID();

    @Test
    @DisplayName("SWR-084: GET /admin/tags returns tag list view")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listTags_returnsTagsView() throws Exception {
        Tag tag = Tag.create(TenantId.of(tenantId), "Java");
        when(tagUseCase.listTags(any(TenantId.class))).thenReturn(List.of(tag));

        mockMvc.perform(get("/admin/tags").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tags"))
                .andExpect(model().attributeExists("tags"));
    }

    @Test
    @DisplayName("SWR-084: GET /admin/tags returns empty list when no tags")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listTags_noTags_returnsEmptyList() throws Exception {
        when(tagUseCase.listTags(any(TenantId.class))).thenReturn(List.of());

        mockMvc.perform(get("/admin/tags").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tags"))
                .andExpect(model().attribute("tags", List.of()));
    }

    @Test
    @DisplayName("SWR-084: POST /admin/tags creates tag and redirects")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createTag_redirectsToList() throws Exception {
        Tag tag = Tag.create(TenantId.of(tenantId), "Spring");
        when(tagUseCase.createTag(any(CreateTagCommand.class))).thenReturn(tag);

        mockMvc.perform(post("/admin/tags")
                        .with(csrf())
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("name", "Spring"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tags"));

        verify(tagUseCase).createTag(any(CreateTagCommand.class));
    }

    @Test
    @DisplayName("SWR-084: POST /admin/tags/{id}/rename renames tag and redirects")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void renameTag_redirectsToList() throws Exception {
        UUID tagId = UUID.randomUUID();
        Tag tag = Tag.create(TenantId.of(tenantId), "Renamed");
        when(tagUseCase.renameTag(any(RenameTagCommand.class))).thenReturn(tag);

        mockMvc.perform(post("/admin/tags/{id}/rename", tagId)
                        .with(csrf())
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("name", "Renamed"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tags"));

        verify(tagUseCase).renameTag(any(RenameTagCommand.class));
    }

    @Test
    @DisplayName("SWR-084: POST /admin/tags/{id}/delete deletes tag and redirects")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteTag_redirectsToList() throws Exception {
        UUID tagId = UUID.randomUUID();

        mockMvc.perform(post("/admin/tags/{id}/delete", tagId).with(csrf()).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tags"));

        verify(tagUseCase).deleteTag(any(TagId.class), any(TenantId.class));
    }

    @Test
    @DisplayName("SWR-084: GET /admin/tags requires authentication")
    void listTags_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/tags").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().is3xxRedirection());
    }
}
