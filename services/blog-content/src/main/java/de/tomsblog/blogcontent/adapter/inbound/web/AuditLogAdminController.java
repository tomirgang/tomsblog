package de.tomsblog.blogcontent.adapter.inbound.web;

import de.tomsblog.blogcontent.application.port.inbound.AuditLogQueryUseCase;
import de.tomsblog.blogcontent.application.port.inbound.AuditLogSearchCriteria;
import de.tomsblog.shared.audit.AuditLogEntry;
import de.tomsblog.shared.tenant.TenantId;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Web adapter for displaying audit logs in the admin area.
 *
 * @req SWR-088
 * @req SWA-036
 */
@Controller
@RequestMapping("/admin/audit-logs")
public class AuditLogAdminController {

    private static final int PAGE_SIZE = 25;

    private final AuditLogQueryUseCase auditLogQueryUseCase;

    public AuditLogAdminController(AuditLogQueryUseCase auditLogQueryUseCase) {
        this.auditLogQueryUseCase = auditLogQueryUseCase;
    }

    /** @req SWR-088 */
    @GetMapping
    public String listAuditLogs(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        TenantId tenant = new TenantId(tenantId);
        PageRequest pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "timestamp"));

        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria(
                blankToNull(action),
                blankToNull(entityType),
                blankToNull(actor),
                parseDate(from, false),
                parseDate(to, true),
                blankToNull(search));

        Page<AuditLogEntry> auditPage;
        if (criteria.hasFilters()) {
            auditPage = auditLogQueryUseCase.search(tenant, criteria, pageable);
        } else {
            auditPage = auditLogQueryUseCase.findAll(tenant, pageable);
        }

        model.addAttribute("auditEntries", auditPage.getContent());
        model.addAttribute("currentPage", auditPage.getNumber());
        model.addAttribute("totalPages", auditPage.getTotalPages());
        model.addAttribute("totalElements", auditPage.getTotalElements());
        model.addAttribute("actions", auditLogQueryUseCase.getDistinctActions(tenant));
        model.addAttribute("entityTypes", auditLogQueryUseCase.getDistinctEntityTypes(tenant));
        model.addAttribute("actors", auditLogQueryUseCase.getDistinctActors(tenant));
        model.addAttribute("selectedAction", action);
        model.addAttribute("selectedEntityType", entityType);
        model.addAttribute("selectedActor", actor);
        model.addAttribute("selectedFrom", from);
        model.addAttribute("selectedTo", to);
        model.addAttribute("selectedSearch", search);

        return "admin/audit-logs";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static Instant parseDate(String date, boolean endOfDay) {
        if (date == null || date.isBlank()) {
            return null;
        }
        LocalDate localDate = LocalDate.parse(date.strip());
        if (endOfDay) {
            return localDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        }
        return localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
