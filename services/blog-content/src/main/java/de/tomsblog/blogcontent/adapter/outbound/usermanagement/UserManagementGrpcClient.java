package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import de.tomsblog.grpc.usermanagement.FindByUsernameRequest;
import de.tomsblog.grpc.usermanagement.GetTenantSettingsRequest;
import de.tomsblog.grpc.usermanagement.SyncOidcUserRequest;
import de.tomsblog.grpc.usermanagement.TenantSettingsServiceGrpc;
import de.tomsblog.grpc.usermanagement.UserManagementServiceGrpc;
import de.tomsblog.grpc.usermanagement.UserProfileResponse;
import java.util.List;
import java.util.UUID;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

/**
 * gRPC client for communication with the User Management Service.
 * Replaces the former REST-based client to fulfill SWR-046 and SWR-049.
 *
 * @req SWR-046
 * @req SWR-043
 * @req SWR-049
 */
@Component
public class UserManagementGrpcClient implements UserManagementClient {

    private final UserManagementServiceGrpc.UserManagementServiceBlockingStub userManagementStub;
    private final TenantSettingsServiceGrpc.TenantSettingsServiceBlockingStub tenantSettingsStub;

    public UserManagementGrpcClient(
            @GrpcClient("user-management")
                    UserManagementServiceGrpc.UserManagementServiceBlockingStub userManagementStub,
            @GrpcClient("user-management")
                    TenantSettingsServiceGrpc.TenantSettingsServiceBlockingStub tenantSettingsStub) {
        this.userManagementStub = userManagementStub;
        this.tenantSettingsStub = tenantSettingsStub;
    }

    /**
     * Synchronizes an OIDC user profile. Creates or updates the profile and returns it with roles.
     */
    public UserProfileDto syncOidcUser(
            String oidcSubject, String email, String displayName, List<String> groups, UUID tenantId) {
        var request = SyncOidcUserRequest.newBuilder()
                .setOidcSubject(oidcSubject)
                .setEmail(email != null ? email : "")
                .setDisplayName(displayName != null ? displayName : "")
                .addAllOidcGroups(groups != null ? groups : List.of())
                .setTenantId(tenantId.toString())
                .build();
        var response = userManagementStub.syncOidcUser(request);
        return toUserProfileDto(response);
    }

    /**
     * Retrieves a user profile by username.
     */
    public UserProfileDto findByUsername(String username) {
        var request = FindByUsernameRequest.newBuilder().setUsername(username).build();
        var response = userManagementStub.findByUsername(request);
        return toUserProfileDto(response);
    }

    /**
     * Retrieves tenant settings (login mode, auto-approval) from the User Management Service.
     */
    public TenantSettingsDto getTenantSettings(UUID tenantId) {
        var request = GetTenantSettingsRequest.newBuilder()
                .setTenantId(tenantId.toString())
                .build();
        var response = tenantSettingsStub.getTenantSettings(request);
        return new TenantSettingsDto(
                UUID.fromString(response.getTenantId()),
                response.getLoginMode(),
                response.getAutoApproveOidc(),
                new java.util.HashSet<>(response.getAutoApproveEmailDomainsList()));
    }

    private UserProfileDto toUserProfileDto(UserProfileResponse response) {
        return new UserProfileDto(
                response.getId().isEmpty() ? null : UUID.fromString(response.getId()),
                response.getOidcSubject().isEmpty() ? null : response.getOidcSubject(),
                response.getUsername().isEmpty() ? null : response.getUsername(),
                response.getAuthSource(),
                response.getEmail().isEmpty() ? null : response.getEmail(),
                response.getDisplayName().isEmpty() ? null : response.getDisplayName(),
                response.getApprovalStatus(),
                response.getGlobalRolesList(),
                response.getTenantMembershipsList().stream()
                        .map(m -> new UserProfileDto.TenantMembershipDto(m.getTenantId(), m.getRole()))
                        .toList());
    }
}
