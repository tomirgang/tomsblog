package de.tomsblog.usermanagement.adapter.inbound.rest;

import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.RegisterUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.SyncInternalUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.SyncOidcUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.UserProfileUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user profile management.
 *
 * @req SWR-043
 * @req SWR-059
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserProfileUseCase userProfileUseCase;

    public UserController(UserProfileUseCase userProfileUseCase) {
        this.userProfileUseCase = userProfileUseCase;
    }

    @PostMapping("/sync/oidc")
    public ResponseEntity<UserProfileResponse> syncFromOidc(@Valid @RequestBody SyncOidcUserRequest request) {
        var command = new SyncOidcUserCommand(
                request.oidcSubject(),
                request.email(),
                request.displayName(),
                request.oidcGroups() != null ? request.oidcGroups() : List.of(),
                TenantId.of(request.tenantId()));
        var profile = userProfileUseCase.syncFromOidc(command);
        return ResponseEntity.ok(UserProfileResponse.from(profile));
    }

    @PostMapping("/sync/internal")
    public ResponseEntity<UserProfileResponse> syncFromInternal(@Valid @RequestBody SyncInternalUserRequest request) {
        var command = new SyncInternalUserCommand(
                request.username(),
                request.passwordHash(),
                request.email(),
                request.displayName(),
                TenantId.of(request.tenantId()));
        var profile = userProfileUseCase.syncFromInternal(command);
        return ResponseEntity.ok(UserProfileResponse.from(profile));
    }

    @GetMapping("/by-oidc-subject/{oidcSubject}")
    public ResponseEntity<UserProfileResponse> findByOidcSubject(@PathVariable String oidcSubject) {
        var profile = userProfileUseCase.findByOidcSubject(oidcSubject);
        return ResponseEntity.ok(UserProfileResponse.from(profile));
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserProfileResponse> findByUsername(@PathVariable String username) {
        var profile = userProfileUseCase.findByUsername(username);
        return ResponseEntity.ok(UserProfileResponse.from(profile));
    }

    @PostMapping("/{identifier}/approve")
    public ResponseEntity<Void> approveUser(@PathVariable String identifier) {
        userProfileUseCase.approveUser(identifier);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{identifier}/reject")
    public ResponseEntity<Void> rejectUser(@PathVariable String identifier) {
        userProfileUseCase.rejectUser(identifier);
        return ResponseEntity.noContent().build();
    }

    /** @req SWR-059 */
    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        var command = new RegisterUserCommand(
                request.username(),
                request.password(),
                request.email(),
                request.displayName(),
                TenantId.of(request.tenantId()));
        var profile = userProfileUseCase.register(command);
        return ResponseEntity.created(URI.create("/api/users/by-username/" + profile.getUsername()))
                .body(UserProfileResponse.from(profile));
    }
}
