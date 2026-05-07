package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import de.tomsblog.grpc.usermanagement.ApproveUserRequest;
import de.tomsblog.grpc.usermanagement.ChangeUserRoleRequest;
import de.tomsblog.grpc.usermanagement.FindByUsernameRequest;
import de.tomsblog.grpc.usermanagement.GetTenantSettingsRequest;
import de.tomsblog.grpc.usermanagement.ListTenantsRequest;
import de.tomsblog.grpc.usermanagement.ListUsersByTenantRequest;
import de.tomsblog.grpc.usermanagement.RegisterUserRequest;
import de.tomsblog.grpc.usermanagement.RejectUserRequest;
import de.tomsblog.grpc.usermanagement.SyncOidcUserRequest;
import de.tomsblog.grpc.usermanagement.TenantSettingsServiceGrpc;
import de.tomsblog.grpc.usermanagement.UpdateTenantSettingsRequest;
import de.tomsblog.grpc.usermanagement.UserManagementServiceGrpc;
import de.tomsblog.grpc.usermanagement.UserProfileResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
 * @req SWR-050
 * @req SWR-051
 * @req SWR-052
 * @req SWR-053
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

    @Override
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

    @Override
    public UserProfileDto findByUsername(String username) {
        var request = FindByUsernameRequest.newBuilder().setUsername(username).build();
        var response = userManagementStub.findByUsername(request);
        return toUserProfileDto(response);
    }

    @Override
    public TenantSettingsDto getTenantSettings(UUID tenantId) {
        var request = GetTenantSettingsRequest.newBuilder()
                .setTenantId(tenantId.toString())
                .build();
        var response = tenantSettingsStub.getTenantSettings(request);
        return new TenantSettingsDto(
                UUID.fromString(response.getTenantId()),
                response.getLoginMode(),
                response.getAutoApproveOidc(),
                new HashSet<>(response.getAutoApproveEmailDomainsList()),
                response.getDisplayName(),
                response.getTagline().isEmpty() ? null : response.getTagline(),
                response.getImpressumContent().isEmpty() ? null : response.getImpressumContent(),
                response.getPrivacyPolicyContent().isEmpty() ? null : response.getPrivacyPolicyContent());
    }

    @Override
    public List<UserProfileDto> listUsersByTenant(UUID tenantId) {
        var request = ListUsersByTenantRequest.newBuilder()
                .setTenantId(tenantId.toString())
                .build();
        var response = userManagementStub.listUsersByTenant(request);
        return response.getUsersList().stream().map(this::toUserProfileDto).toList();
    }

    @Override
    public void approveUser(String identifier) {
        var request = ApproveUserRequest.newBuilder().setIdentifier(identifier).build();
        userManagementStub.approveUser(request);
    }

    @Override
    public void rejectUser(String identifier) {
        var request = RejectUserRequest.newBuilder().setIdentifier(identifier).build();
        userManagementStub.rejectUser(request);
    }

    @Override
    public void changeUserRole(String identifier, UUID tenantId, String role) {
        var request = ChangeUserRoleRequest.newBuilder()
                .setIdentifier(identifier)
                .setTenantId(tenantId.toString())
                .setRole(role)
                .build();
        userManagementStub.changeUserRole(request);
    }

    @Override
    public TenantSettingsDto updateTenantSettings(
            UUID tenantId,
            String loginMode,
            boolean autoApproveOidc,
            Set<String> autoApproveEmailDomains,
            String displayName,
            String tagline,
            String impressumContent,
            String privacyPolicyContent) {
        var request = UpdateTenantSettingsRequest.newBuilder()
                .setTenantId(tenantId.toString())
                .setLoginMode(loginMode)
                .setAutoApproveOidc(autoApproveOidc)
                .addAllAutoApproveEmailDomains(autoApproveEmailDomains)
                .setDisplayName(displayName != null ? displayName : "")
                .setTagline(tagline != null ? tagline : "")
                .setImpressumContent(impressumContent != null ? impressumContent : "")
                .setPrivacyPolicyContent(privacyPolicyContent != null ? privacyPolicyContent : "")
                .build();
        var response = tenantSettingsStub.updateTenantSettings(request);
        return new TenantSettingsDto(
                UUID.fromString(response.getTenantId()),
                response.getLoginMode(),
                response.getAutoApproveOidc(),
                new HashSet<>(response.getAutoApproveEmailDomainsList()),
                response.getDisplayName(),
                response.getTagline().isEmpty() ? null : response.getTagline(),
                response.getImpressumContent().isEmpty() ? null : response.getImpressumContent(),
                response.getPrivacyPolicyContent().isEmpty() ? null : response.getPrivacyPolicyContent());
    }

    @Override
    public List<TenantInfoDto> listTenants() {
        var response = tenantSettingsStub.listTenants(ListTenantsRequest.getDefaultInstance());
        return response.getTenantsList().stream()
                .map(t -> new TenantInfoDto(UUID.fromString(t.getTenantId()), t.getDisplayName()))
                .toList();
    }

    @Override
    public UserProfileDto registerUser(
            String username, String password, String email, String displayName, UUID tenantId) {
        var request = RegisterUserRequest.newBuilder()
                .setUsername(username)
                .setPassword(password)
                .setEmail(email != null ? email : "")
                .setDisplayName(displayName != null ? displayName : "")
                .setTenantId(tenantId.toString())
                .build();
        var response = userManagementStub.registerUser(request);
        return toUserProfileDto(response);
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
