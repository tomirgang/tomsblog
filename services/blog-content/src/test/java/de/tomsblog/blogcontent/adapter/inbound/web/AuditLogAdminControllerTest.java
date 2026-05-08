package de.tomsblog.blogcontent.adapter.inbound.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import de.tomsblog.blogcontent.application.port.inbound.AuditLogQueryUseCase;
import de.tomsblog.blogcontent.application.port.inbound.AuditLogSearchCriteria;
import de.tomsblog.shared.audit.AuditLogEntry;
import de.tomsblog.shared.tenant.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuditLogAdminController.class)
@Import(SecurityConfiguration.class)
class AuditLogAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogQueryUseCase auditLogQueryUseCase;

    @MockitoBean
    private UserManagementClient userManagementClient;

    private final UUID tenantId = UUID.randomUUID();

    @Test
    @DisplayName("SWR-088: GET /admin/audit-logs returns audit log view")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listAuditLogs_returnsView() throws Exception {
        AuditLogEntry entry = AuditLogEntry.create(tenantId.toString(), "admin", "POST_CREATED", "Post", "123");
        Page<AuditLogEntry> page = new PageImpl<>(List.of(entry), PageRequest.of(0, 25), 1);
        when(auditLogQueryUseCase.findAll(any(TenantId.class), any(Pageable.class)))
                .thenReturn(page);
        when(auditLogQueryUseCase.getDistinctActions(any(TenantId.class))).thenReturn(List.of("POST_CREATED"));
        when(auditLogQueryUseCase.getDistinctEntityTypes(any(TenantId.class))).thenReturn(List.of("Post"));
        when(auditLogQueryUseCase.getDistinctActors(any(TenantId.class))).thenReturn(List.of("admin"));

        mockMvc.perform(get("/admin/audit-logs").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/audit-logs"))
                .andExpect(model().attributeExists("auditEntries"))
                .andExpect(model().attributeExists("actions"))
                .andExpect(model().attributeExists("entityTypes"))
                .andExpect(model().attributeExists("actors"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("totalPages"));
    }

    @Test
    @DisplayName("SWR-088: GET /admin/audit-logs with filters uses search")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listAuditLogs_withFilters_usesSearch() throws Exception {
        Page<AuditLogEntry> page = new PageImpl<>(List.of(), PageRequest.of(0, 25), 0);
        when(auditLogQueryUseCase.search(any(TenantId.class), any(AuditLogSearchCriteria.class), any(Pageable.class)))
                .thenReturn(page);
        when(auditLogQueryUseCase.getDistinctActions(any(TenantId.class))).thenReturn(List.of());
        when(auditLogQueryUseCase.getDistinctEntityTypes(any(TenantId.class))).thenReturn(List.of());
        when(auditLogQueryUseCase.getDistinctActors(any(TenantId.class))).thenReturn(List.of());

        mockMvc.perform(get("/admin/audit-logs")
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("action", "POST_CREATED")
                        .param("entityType", "Post"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/audit-logs"))
                .andExpect(model().attribute("selectedAction", "POST_CREATED"))
                .andExpect(model().attribute("selectedEntityType", "Post"));

        verify(auditLogQueryUseCase)
                .search(any(TenantId.class), any(AuditLogSearchCriteria.class), any(Pageable.class));
        verify(auditLogQueryUseCase, never()).findAll(any(TenantId.class), any(Pageable.class));
    }

    @Test
    @DisplayName("SWR-088: GET /admin/audit-logs with date filter parses dates")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listAuditLogs_withDateFilter_parsesDates() throws Exception {
        Page<AuditLogEntry> page = new PageImpl<>(List.of(), PageRequest.of(0, 25), 0);
        when(auditLogQueryUseCase.search(any(TenantId.class), any(AuditLogSearchCriteria.class), any(Pageable.class)))
                .thenReturn(page);
        when(auditLogQueryUseCase.getDistinctActions(any(TenantId.class))).thenReturn(List.of());
        when(auditLogQueryUseCase.getDistinctEntityTypes(any(TenantId.class))).thenReturn(List.of());
        when(auditLogQueryUseCase.getDistinctActors(any(TenantId.class))).thenReturn(List.of());

        mockMvc.perform(get("/admin/audit-logs")
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/audit-logs"));

        verify(auditLogQueryUseCase)
                .search(
                        any(TenantId.class),
                        argThat(criteria -> criteria.from() != null
                                && criteria.to() != null
                                && criteria.from().equals(Instant.parse("2026-01-01T00:00:00Z"))
                                && criteria.to().equals(Instant.parse("2026-02-01T00:00:00Z"))),
                        any(Pageable.class));
    }

    @Test
    @DisplayName("SWR-088: GET /admin/audit-logs with search term uses search")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listAuditLogs_withSearchTerm_usesSearch() throws Exception {
        Page<AuditLogEntry> page = new PageImpl<>(List.of(), PageRequest.of(0, 25), 0);
        when(auditLogQueryUseCase.search(any(TenantId.class), any(AuditLogSearchCriteria.class), any(Pageable.class)))
                .thenReturn(page);
        when(auditLogQueryUseCase.getDistinctActions(any(TenantId.class))).thenReturn(List.of());
        when(auditLogQueryUseCase.getDistinctEntityTypes(any(TenantId.class))).thenReturn(List.of());
        when(auditLogQueryUseCase.getDistinctActors(any(TenantId.class))).thenReturn(List.of());

        mockMvc.perform(get("/admin/audit-logs")
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("search", "some query"))
                .andExpect(status().isOk());

        verify(auditLogQueryUseCase)
                .search(
                        any(TenantId.class),
                        argThat(criteria -> "some query".equals(criteria.searchTerm())),
                        any(Pageable.class));
    }

    @Test
    @DisplayName("SWR-088: GET /admin/audit-logs returns empty list when no entries")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listAuditLogs_noEntries_returnsEmptyList() throws Exception {
        Page<AuditLogEntry> page = new PageImpl<>(List.of(), PageRequest.of(0, 25), 0);
        when(auditLogQueryUseCase.findAll(any(TenantId.class), any(Pageable.class)))
                .thenReturn(page);
        when(auditLogQueryUseCase.getDistinctActions(any(TenantId.class))).thenReturn(List.of());
        when(auditLogQueryUseCase.getDistinctEntityTypes(any(TenantId.class))).thenReturn(List.of());
        when(auditLogQueryUseCase.getDistinctActors(any(TenantId.class))).thenReturn(List.of());

        mockMvc.perform(get("/admin/audit-logs").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("auditEntries", List.of()))
                .andExpect(model().attribute("totalElements", 0L));
    }

    @Test
    @DisplayName("SWR-088: GET /admin/audit-logs requires ADMIN role")
    @WithMockUser(username = "reader", roles = "READER")
    void listAuditLogs_nonAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/admin/audit-logs").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SWR-088: GET /admin/audit-logs requires authentication")
    void listAuditLogs_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/audit-logs").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("SWR-088: GET /admin/audit-logs accessible by SUPERADMIN")
    @WithMockUser(username = "superadmin", roles = "SUPERADMIN")
    void listAuditLogs_superAdmin_succeeds() throws Exception {
        Page<AuditLogEntry> page = new PageImpl<>(List.of(), PageRequest.of(0, 25), 0);
        when(auditLogQueryUseCase.findAll(any(TenantId.class), any(Pageable.class)))
                .thenReturn(page);
        when(auditLogQueryUseCase.getDistinctActions(any(TenantId.class))).thenReturn(List.of());
        when(auditLogQueryUseCase.getDistinctEntityTypes(any(TenantId.class))).thenReturn(List.of());
        when(auditLogQueryUseCase.getDistinctActors(any(TenantId.class))).thenReturn(List.of());

        mockMvc.perform(get("/admin/audit-logs").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SWR-088: GET /admin/audit-logs with page parameter navigates pages")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listAuditLogs_withPageParam_navigatesPages() throws Exception {
        Page<AuditLogEntry> page = new PageImpl<>(List.of(), PageRequest.of(2, 25), 100);
        when(auditLogQueryUseCase.findAll(any(TenantId.class), any(Pageable.class)))
                .thenReturn(page);
        when(auditLogQueryUseCase.getDistinctActions(any(TenantId.class))).thenReturn(List.of());
        when(auditLogQueryUseCase.getDistinctEntityTypes(any(TenantId.class))).thenReturn(List.of());
        when(auditLogQueryUseCase.getDistinctActors(any(TenantId.class))).thenReturn(List.of());

        mockMvc.perform(get("/admin/audit-logs")
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 2))
                .andExpect(model().attribute("totalPages", 4));
    }

    @Test
    @DisplayName("SWR-088: GET /admin/audit-logs with blank params treats as no filter")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listAuditLogs_blankParams_treatedAsNoFilter() throws Exception {
        Page<AuditLogEntry> page = new PageImpl<>(List.of(), PageRequest.of(0, 25), 0);
        when(auditLogQueryUseCase.findAll(any(TenantId.class), any(Pageable.class)))
                .thenReturn(page);
        when(auditLogQueryUseCase.getDistinctActions(any(TenantId.class))).thenReturn(List.of());
        when(auditLogQueryUseCase.getDistinctEntityTypes(any(TenantId.class))).thenReturn(List.of());
        when(auditLogQueryUseCase.getDistinctActors(any(TenantId.class))).thenReturn(List.of());

        mockMvc.perform(get("/admin/audit-logs")
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("action", "")
                        .param("search", "  "))
                .andExpect(status().isOk());

        verify(auditLogQueryUseCase).findAll(any(TenantId.class), any(Pageable.class));
        verify(auditLogQueryUseCase, never())
                .search(any(TenantId.class), any(AuditLogSearchCriteria.class), any(Pageable.class));
    }

    @Test
    @DisplayName("SWR-088: GET /admin/audit-logs with blank date params treats as no filter")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listAuditLogs_blankDateParams_treatedAsNoFilter() throws Exception {
        Page<AuditLogEntry> page = new PageImpl<>(List.of(), PageRequest.of(0, 25), 0);
        when(auditLogQueryUseCase.findAll(any(TenantId.class), any(Pageable.class)))
                .thenReturn(page);
        when(auditLogQueryUseCase.getDistinctActions(any(TenantId.class))).thenReturn(List.of());
        when(auditLogQueryUseCase.getDistinctEntityTypes(any(TenantId.class))).thenReturn(List.of());
        when(auditLogQueryUseCase.getDistinctActors(any(TenantId.class))).thenReturn(List.of());

        mockMvc.perform(get("/admin/audit-logs")
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("from", "")
                        .param("to", " "))
                .andExpect(status().isOk());

        verify(auditLogQueryUseCase).findAll(any(TenantId.class), any(Pageable.class));
    }
}
