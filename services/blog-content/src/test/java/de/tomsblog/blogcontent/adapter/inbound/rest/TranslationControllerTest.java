package de.tomsblog.blogcontent.adapter.inbound.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.tomsblog.blogcontent.application.port.inbound.TranslationUseCase;
import de.tomsblog.blogcontent.application.service.TranslationNotFoundException;
import de.tomsblog.blogcontent.domain.model.*;
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

@WebMvcTest(TranslationController.class)
@SuppressWarnings("null")
class TranslationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TranslationUseCase translationUseCase;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID postId = UUID.randomUUID();

    @Test
    @DisplayName("POST /api/posts/{postId}/translations creates translation and returns 201")
    void createTranslation_returns201() throws Exception {
        Translation translation = Translation.createManual(
                PostId.of(postId), TenantId.of(tenantId), PostLocale.english(), "English Title", "English Content");
        when(translationUseCase.createManualTranslation(any())).thenReturn(translation);

        mockMvc.perform(post("/api/posts/{postId}/translations", postId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"locale": "en", "title": "English Title", "content": "English Content"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("English Title"))
                .andExpect(jsonPath("$.locale").value("en"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("GET /api/posts/{postId}/translations returns list")
    void listTranslations_returns200() throws Exception {
        Translation t = Translation.createManual(
                PostId.of(postId), TenantId.of(tenantId), PostLocale.english(), "Title", "Content");
        when(translationUseCase.listTranslations(any(), any())).thenReturn(List.of(t));

        mockMvc.perform(get("/api/posts/{postId}/translations", postId).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/posts/{postId}/translations/{id} returns 404 when not found")
    void getTranslation_returns404() throws Exception {
        TranslationId translationId = TranslationId.generate();
        when(translationUseCase.getTranslation(any(), any()))
                .thenThrow(new TranslationNotFoundException(translationId));

        mockMvc.perform(get("/api/posts/{postId}/translations/{id}", postId, translationId.value())
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/posts/{postId}/translations/{id}/approve returns 204")
    void approveTranslation_returns204() throws Exception {
        UUID translationId = UUID.randomUUID();
        doNothing().when(translationUseCase).approveTranslation(any(), any());

        mockMvc.perform(post("/api/posts/{postId}/translations/{id}/approve", postId, translationId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/posts/{postId}/translations/{id} returns 204")
    void deleteTranslation_returns204() throws Exception {
        UUID translationId = UUID.randomUUID();
        doNothing().when(translationUseCase).deleteTranslation(any(), any());

        mockMvc.perform(delete("/api/posts/{postId}/translations/{id}", postId, translationId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNoContent());
    }
}
