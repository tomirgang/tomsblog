package de.tomsblog.usermanagement.adapter.inbound.rest;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for tenant settings management.
 *
 * @req SWR-044
 * @req SWR-045
 */
@RestController
@RequestMapping("/api/tenants/{tenantId}/settings")
public class TenantSettingsController {

    private final TenantSettingsUseCase tenantSettingsUseCase;

    public TenantSettingsController(TenantSettingsUseCase tenantSettingsUseCase) {
        this.tenantSettingsUseCase = tenantSettingsUseCase;
    }

    @GetMapping
    public ResponseEntity<TenantSettingsResponse> getSettings(@PathVariable UUID tenantId) {
        var settings = tenantSettingsUseCase.getSettings(TenantId.of(tenantId));
        return ResponseEntity.ok(TenantSettingsResponse.from(settings));
    }

    @PutMapping("/login-mode")
    public ResponseEntity<TenantSettingsResponse> updateLoginMode(
            @PathVariable UUID tenantId, @Valid @RequestBody UpdateLoginModeRequest request) {
        var settings = tenantSettingsUseCase.updateLoginMode(TenantId.of(tenantId), request.loginMode());
        return ResponseEntity.ok(TenantSettingsResponse.from(settings));
    }

    @PutMapping("/auto-approval")
    public ResponseEntity<TenantSettingsResponse> updateAutoApproval(
            @PathVariable UUID tenantId, @Valid @RequestBody UpdateAutoApprovalRequest request) {
        var settings = tenantSettingsUseCase.updateAutoApproval(
                TenantId.of(tenantId),
                request.autoApproveOidc(),
                request.autoApproveEmailDomains() != null ? request.autoApproveEmailDomains() : Set.of());
        return ResponseEntity.ok(TenantSettingsResponse.from(settings));
    }

    public record UpdateLoginModeRequest(@NotNull LoginMode loginMode) {}

    public record UpdateAutoApprovalRequest(boolean autoApproveOidc, Set<String> autoApproveEmailDomains) {}

    public record TenantSettingsResponse(
            UUID tenantId, LoginMode loginMode, boolean autoApproveOidc, Set<String> autoApproveEmailDomains) {

        public static TenantSettingsResponse from(TenantSettings settings) {
            return new TenantSettingsResponse(
                    settings.getTenantId().value(),
                    settings.getLoginMode(),
                    settings.isAutoApproveOidc(),
                    settings.getAutoApproveEmailDomains());
        }
    }
}
