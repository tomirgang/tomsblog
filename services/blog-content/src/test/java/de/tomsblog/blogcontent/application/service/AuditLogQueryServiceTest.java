package de.tomsblog.blogcontent.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import de.tomsblog.blogcontent.application.port.inbound.AuditLogSearchCriteria;
import de.tomsblog.blogcontent.application.port.outbound.AuditLogQueryRepository;
import de.tomsblog.shared.audit.AuditLogEntry;
import de.tomsblog.shared.tenant.TenantId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AuditLogQueryServiceTest {

    @Mock
    private AuditLogQueryRepository repository;

    private AuditLogQueryService service;

    private final TenantId tenantId = TenantId.of(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        service = new AuditLogQueryService(repository);
    }

    @Test
    @DisplayName("SWR-087: findAll delegates to repository with tenant ID")
    void findAll_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 25);
        AuditLogEntry entry = AuditLogEntry.create(tenantId.value().toString(), "admin", "POST_CREATED", "Post", "123");
        Page<AuditLogEntry> expected = new PageImpl<>(List.of(entry), pageable, 1);
        when(repository.findByTenantId(tenantId.value().toString(), pageable)).thenReturn(expected);

        Page<AuditLogEntry> result = service.findAll(tenantId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().action()).isEqualTo("POST_CREATED");
        verify(repository).findByTenantId(tenantId.value().toString(), pageable);
    }

    @Test
    @DisplayName("SWR-087: search delegates to repository with criteria")
    void search_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 25);
        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria("POST_CREATED", null, null, null, null, null);
        Page<AuditLogEntry> expected = new PageImpl<>(List.of(), pageable, 0);
        when(repository.findByTenantIdAndCriteria(tenantId.value().toString(), criteria, pageable))
                .thenReturn(expected);

        Page<AuditLogEntry> result = service.search(tenantId, criteria, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(repository).findByTenantIdAndCriteria(tenantId.value().toString(), criteria, pageable);
    }

    @Test
    @DisplayName("SWR-087: getDistinctActions delegates to repository")
    void getDistinctActions_delegatesToRepository() {
        when(repository.findDistinctActions(tenantId.value().toString()))
                .thenReturn(List.of("POST_CREATED", "TAG_CREATED"));

        List<String> result = service.getDistinctActions(tenantId);

        assertThat(result).containsExactly("POST_CREATED", "TAG_CREATED");
    }

    @Test
    @DisplayName("SWR-087: getDistinctEntityTypes delegates to repository")
    void getDistinctEntityTypes_delegatesToRepository() {
        when(repository.findDistinctEntityTypes(tenantId.value().toString())).thenReturn(List.of("Post", "Tag"));

        List<String> result = service.getDistinctEntityTypes(tenantId);

        assertThat(result).containsExactly("Post", "Tag");
    }

    @Test
    @DisplayName("SWR-087: getDistinctActors delegates to repository")
    void getDistinctActors_delegatesToRepository() {
        when(repository.findDistinctActors(tenantId.value().toString())).thenReturn(List.of("admin", "system"));

        List<String> result = service.getDistinctActors(tenantId);

        assertThat(result).containsExactly("admin", "system");
    }
}
