package de.tomsblog.blogcontent.adapter.inbound.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.tomsblog.blogcontent.application.port.inbound.TagUseCase;
import de.tomsblog.blogcontent.application.service.TagNotFoundException;
import de.tomsblog.blogcontent.domain.model.Tag;
import de.tomsblog.blogcontent.domain.model.TagId;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TagController.class)
@SuppressWarnings("null")
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagUseCase tagUseCase;

    private final UUID tenantId = UUID.randomUUID();

    @Test
    @DisplayName("POST /api/tags creates tag and returns 201")
    void createTag_returns201() throws Exception {
        Tag tag = Tag.create(TenantId.of(tenantId), "Java");
        when(tagUseCase.createTag(any())).thenReturn(tag);

        mockMvc.perform(post("/api/tags")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Java"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Java"))
                .andExpect(jsonPath("$.slug").value("java"));
    }

    @Test
    @DisplayName("GET /api/tags returns all tags for tenant")
    void listTags_returns200() throws Exception {
        Tag tag1 = Tag.create(TenantId.of(tenantId), "Java");
        Tag tag2 = Tag.create(TenantId.of(tenantId), "Spring");
        when(tagUseCase.listTags(any())).thenReturn(List.of(tag1, tag2));

        mockMvc.perform(get("/api/tags").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/tags/{id} returns 404 when not found")
    void getTag_returns404() throws Exception {
        TagId tagId = TagId.generate();
        when(tagUseCase.getTag(any(), any())).thenThrow(new TagNotFoundException(tagId));

        mockMvc.perform(get("/api/tags/{id}", tagId.value()).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/tags/{id} returns tag")
    void getTag_returns200() throws Exception {
        Tag tag = Tag.create(TenantId.of(tenantId), "Spring");
        when(tagUseCase.getTag(any(), any())).thenReturn(tag);

        mockMvc.perform(get("/api/tags/{id}", tag.getId().value()).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Spring"))
                .andExpect(jsonPath("$.slug").value("spring"));
    }

    @Test
    @DisplayName("PUT /api/tags/{id} renames tag")
    void renameTag_returns200() throws Exception {
        Tag tag = Tag.create(TenantId.of(tenantId), "Java");
        tag.rename("Kotlin");
        when(tagUseCase.renameTag(any())).thenReturn(tag);

        mockMvc.perform(put("/api/tags/{id}", tag.getId().value())
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Kotlin"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kotlin"))
                .andExpect(jsonPath("$.slug").value("kotlin"));
    }

    @Test
    @DisplayName("DELETE /api/tags/{id} returns 204")
    void deleteTag_returns204() throws Exception {
        UUID tagId = UUID.randomUUID();
        doNothing().when(tagUseCase).deleteTag(any(), any());

        mockMvc.perform(delete("/api/tags/{id}", tagId).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/tags with blank name returns 400")
    void createTag_returns400WhenNameBlank() throws Exception {
        mockMvc.perform(post("/api/tags")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": ""}
                                """))
                .andExpect(status().isBadRequest());
    }
}
