package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tomsblog.grpc.usermanagement.ApproveUserRequest;
import de.tomsblog.grpc.usermanagement.ChangeUserRoleRequest;
import de.tomsblog.grpc.usermanagement.FindByUsernameRequest;
import de.tomsblog.grpc.usermanagement.GetTenantSettingsRequest;
import de.tomsblog.grpc.usermanagement.ListTenantsRequest;
import de.tomsblog.grpc.usermanagement.ListTenantsResponse;
import de.tomsblog.grpc.usermanagement.ListUsersByTenantRequest;
import de.tomsblog.grpc.usermanagement.ListUsersResponse;
import de.tomsblog.grpc.usermanagement.RegisterUserRequest;
import de.tomsblog.grpc.usermanagement.RejectUserRequest;
import de.tomsblog.grpc.usermanagement.SyncOidcUserRequest;
import de.tomsblog.grpc.usermanagement.TenantInfoResponse;
import de.tomsblog.grpc.usermanagement.TenantSettingsResponse;
import de.tomsblog.grpc.usermanagement.TenantSettingsServiceGrpc;
import de.tomsblog.grpc.usermanagement.UpdateTenantSettingsRequest;
import de.tomsblog.grpc.usermanagement.UserManagementServiceGrpc;
import de.tomsblog.grpc.usermanagement.UserProfileResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link UserManagementGrpcClient}.
 *
 * @req SWR-043
 * @req SWR-046
 */
@ExtendWith(MockitoExtension.class)
class UserManagementClientTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private UserManagementServiceGrpc.UserManagementServiceBlockingStub userManagementStub;

    @Mock
    private TenantSettingsServiceGrpc.TenantSettingsServiceBlockingStub tenantSettingsStub;

    private UserManagementGrpcClient client;

    @BeforeEach
    void setUp() {
        client = new UserManagementGrpcClient(userManagementStub, tenantSettingsStub);
    }

    @Test
    @DisplayName("SWR-046: syncOidcUser sends gRPC request and returns user profile")
    void syncOidcUser_sendsGrpcAndReturnsProfile() {
        UUID id = UUID.randomUUID();
        var response = UserProfileResponse.newBuilder()
                .setId(id.toString())
                .setOidcSubject("sub-123")
                .setAuthSource("OIDC")
                .setEmail("user@test.com")
                .setDisplayName("Test User")
                .setApprovalStatus("APPROVED")
                .addGlobalRoles("ADMIN")
                .addGlobalRoles("AUTHOR")
                .build();

        when(userManagementStub.syncOidcUser(any(SyncOidcUserRequest.class))).thenReturn(response);

        UserProfileDto result =
                client.syncOidcUser("sub-123", "user@test.com", "Test User", List.of("group1"), TENANT_ID);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.oidcSubject()).isEqualTo("sub-123");
        assertThat(result.globalRoles()).containsExactly("ADMIN", "AUTHOR");
    }

    @Test
    @DisplayName("SWR-046: findByUsername sends gRPC request and returns user profile")
    void findByUsername_sendsGrpcAndReturnsProfile() {
        UUID id = UUID.randomUUID();
        var response = UserProfileResponse.newBuilder()
                .setId(id.toString())
                .setUsername("admin")
                .setAuthSource("INTERNAL")
                .setEmail("admin@test.com")
                .setDisplayName("Admin")
                .setApprovalStatus("APPROVED")
                .addGlobalRoles("SUPERADMIN")
                .build();

        when(userManagementStub.findByUsername(any(FindByUsernameRequest.class)))
                .thenReturn(response);

        UserProfileDto result = client.findByUsername("admin");

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("admin");
        assertThat(result.authSource()).isEqualTo("INTERNAL");
    }

    @Test
    @DisplayName("SWR-046: getTenantSettings returns settings via gRPC")
    void getTenantSettings_returnsSettingsViaGrpc() {
        var response = TenantSettingsResponse.newBuilder()
                .setTenantId(TENANT_ID.toString())
                .setLoginMode("OIDC_ONLY")
                .setAutoApproveOidc(true)
                .addAutoApproveEmailDomains("example.com")
                .build();

        when(tenantSettingsStub.getTenantSettings(any(GetTenantSettingsRequest.class)))
                .thenReturn(response);

        TenantSettingsDto result = client.getTenantSettings(TENANT_ID);

        assertThat(result).isNotNull();
        assertThat(result.tenantId()).isEqualTo(TENANT_ID);
        assertThat(result.loginMode()).isEqualTo("OIDC_ONLY");
        assertThat(result.autoApproveOidc()).isTrue();
        assertThat(result.autoApproveEmailDomains()).containsExactly("example.com");
    }

    @Test
    @DisplayName("SWR-046: syncOidcUser propagates gRPC errors")
    void syncOidcUser_propagatesGrpcErrors() {
        when(userManagementStub.syncOidcUser(any(SyncOidcUserRequest.class)))
                .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        assertThatThrownBy(() -> client.syncOidcUser("sub-err", "err@test.com", "Error", List.of(), TENANT_ID))
                .isInstanceOf(StatusRuntimeException.class);
    }

    @Test
    @DisplayName("SWR-046: syncOidcUser handles null email and displayName")
    void syncOidcUser_handlesNullFields() {
        UUID id = UUID.randomUUID();
        var response = UserProfileResponse.newBuilder()
                .setId(id.toString())
                .setOidcSubject("sub-null")
                .setAuthSource("OIDC")
                .setApprovalStatus("PENDING")
                .addGlobalRoles("READER")
                .build();

        when(userManagementStub.syncOidcUser(any(SyncOidcUserRequest.class))).thenReturn(response);

        UserProfileDto result = client.syncOidcUser("sub-null", null, null, null, TENANT_ID);

        assertThat(result).isNotNull();
        assertThat(result.email()).isNull();
        assertThat(result.displayName()).isNull();
    }

    @Test
    @DisplayName("SWR-051: listUsersByTenant returns user list")
    void listUsersByTenant_returnsList() {
        var response = ListUsersResponse.newBuilder()
                .addUsers(UserProfileResponse.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setOidcSubject("sub-1")
                        .setAuthSource("OIDC")
                        .setApprovalStatus("APPROVED")
                        .build())
                .build();
        when(userManagementStub.listUsersByTenant(any(ListUsersByTenantRequest.class)))
                .thenReturn(response);

        List<UserProfileDto> result = client.listUsersByTenant(TENANT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).oidcSubject()).isEqualTo("sub-1");
    }

    @Test
    @DisplayName("SWR-051: approveUser calls gRPC")
    void approveUser_callsGrpc() {
        var response = UserProfileResponse.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setOidcSubject("sub-1")
                .setAuthSource("OIDC")
                .setApprovalStatus("APPROVED")
                .build();
        when(userManagementStub.approveUser(any(ApproveUserRequest.class))).thenReturn(response);

        client.approveUser("sub-1");

        verify(userManagementStub).approveUser(any(ApproveUserRequest.class));
    }

    @Test
    @DisplayName("SWR-051: rejectUser calls gRPC")
    void rejectUser_callsGrpc() {
        var response = UserProfileResponse.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setOidcSubject("sub-1")
                .setAuthSource("OIDC")
                .setApprovalStatus("REJECTED")
                .build();
        when(userManagementStub.rejectUser(any(RejectUserRequest.class))).thenReturn(response);

        client.rejectUser("sub-1");

        verify(userManagementStub).rejectUser(any(RejectUserRequest.class));
    }

    @Test
    @DisplayName("SWR-051: changeUserRole calls gRPC")
    void changeUserRole_callsGrpc() {
        var response = UserProfileResponse.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setOidcSubject("sub-1")
                .setAuthSource("OIDC")
                .setApprovalStatus("APPROVED")
                .build();
        when(userManagementStub.changeUserRole(any(ChangeUserRoleRequest.class)))
                .thenReturn(response);

        client.changeUserRole("sub-1", TENANT_ID, "ADMIN");

        verify(userManagementStub).changeUserRole(any(ChangeUserRoleRequest.class));
    }

    @Test
    @DisplayName("SWR-052: updateTenantSettings sends gRPC and returns result")
    void updateTenantSettings_returnsSettings() {
        var response = TenantSettingsResponse.newBuilder()
                .setTenantId(TENANT_ID.toString())
                .setLoginMode("OIDC")
                .setAutoApproveOidc(true)
                .addAutoApproveEmailDomains("test.com")
                .setDisplayName("My Blog")
                .setTagline("Cool blog")
                .build();
        when(tenantSettingsStub.updateTenantSettings(any(UpdateTenantSettingsRequest.class)))
                .thenReturn(response);

        TenantSettingsDto result = client.updateTenantSettings(
                TENANT_ID, "OIDC", true, Set.of("test.com"), "My Blog", "Cool blog", null, null, null, null, null);

        assertThat(result.loginMode()).isEqualTo("OIDC");
        assertThat(result.displayName()).isEqualTo("My Blog");
        assertThat(result.tagline()).isEqualTo("Cool blog");
    }

    @Test
    @DisplayName("SWR-052: updateTenantSettings handles null displayName and tagline")
    void updateTenantSettings_handlesNulls() {
        var response = TenantSettingsResponse.newBuilder()
                .setTenantId(TENANT_ID.toString())
                .setLoginMode("BOTH")
                .setDisplayName("Blog")
                .setTagline("")
                .build();
        when(tenantSettingsStub.updateTenantSettings(any(UpdateTenantSettingsRequest.class)))
                .thenReturn(response);

        TenantSettingsDto result = client.updateTenantSettings(
                TENANT_ID, "BOTH", false, Set.of(), null, null, null, null, null, null, null);

        assertThat(result.tagline()).isNull();
    }

    @Test
    @DisplayName("SWR-053: listTenants returns tenant info list")
    void listTenants_returnsList() {
        var response = ListTenantsResponse.newBuilder()
                .addTenants(TenantInfoResponse.newBuilder()
                        .setTenantId(TENANT_ID.toString())
                        .setDisplayName("Blog A")
                        .build())
                .build();
        when(tenantSettingsStub.listTenants(any(ListTenantsRequest.class))).thenReturn(response);

        List<TenantInfoDto> result = client.listTenants();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tenantId()).isEqualTo(TENANT_ID);
        assertThat(result.get(0).displayName()).isEqualTo("Blog A");
    }

    @Test
    @DisplayName("SWR-061: updateTenantSettings passes OIDC fields and maps response")
    void updateTenantSettings_withOidcFields() {
        var response = TenantSettingsResponse.newBuilder()
                .setTenantId(TENANT_ID.toString())
                .setLoginMode("OIDC")
                .setDisplayName("Blog")
                .setOidcIssuerUrl("https://auth.example.com")
                .setOidcClientId("client-123")
                .setOidcClientSecret("secret-456")
                .build();
        when(tenantSettingsStub.updateTenantSettings(any(UpdateTenantSettingsRequest.class)))
                .thenReturn(response);

        TenantSettingsDto result = client.updateTenantSettings(
                TENANT_ID,
                "OIDC",
                false,
                Set.of(),
                "Blog",
                null,
                null,
                null,
                "https://auth.example.com",
                "client-123",
                "secret-456");

        assertThat(result.oidcIssuerUrl()).isEqualTo("https://auth.example.com");
        assertThat(result.oidcClientId()).isEqualTo("client-123");
        assertThat(result.oidcClientSecret()).isEqualTo("secret-456");
    }

    @Test
    @DisplayName("SWR-046: getTenantSettings maps displayName and empty tagline to null")
    void getTenantSettings_mapsEmptyTaglineToNull() {
        var response = TenantSettingsResponse.newBuilder()
                .setTenantId(TENANT_ID.toString())
                .setLoginMode("BOTH")
                .setDisplayName("My Blog")
                .setTagline("")
                .build();
        when(tenantSettingsStub.getTenantSettings(any(GetTenantSettingsRequest.class)))
                .thenReturn(response);

        TenantSettingsDto result = client.getTenantSettings(TENANT_ID);

        assertThat(result.displayName()).isEqualTo("My Blog");
        assertThat(result.tagline()).isNull();
    }

    @Test
    @DisplayName("SWR-059: registerUser sends gRPC request and returns user profile")
    void registerUser_sendsGrpcAndReturnsProfile() {
        UUID id = UUID.randomUUID();
        var response = UserProfileResponse.newBuilder()
                .setId(id.toString())
                .setUsername("newuser")
                .setAuthSource("INTERNAL")
                .setEmail("new@test.com")
                .setDisplayName("New User")
                .setApprovalStatus("PENDING")
                .addGlobalRoles("READER")
                .build();

        when(userManagementStub.registerUser(any(RegisterUserRequest.class))).thenReturn(response);

        UserProfileDto result = client.registerUser("newuser", "securePassw0rd", "new@test.com", "New User", TENANT_ID);

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("newuser");
        assertThat(result.approvalStatus()).isEqualTo("PENDING");
        verify(userManagementStub).registerUser(any(RegisterUserRequest.class));
    }

    @Test
    @DisplayName("SWR-059: registerUser handles null email and displayName")
    void registerUser_handlesNullFields() {
        UUID id = UUID.randomUUID();
        var response = UserProfileResponse.newBuilder()
                .setId(id.toString())
                .setUsername("newuser")
                .setAuthSource("INTERNAL")
                .setApprovalStatus("PENDING")
                .addGlobalRoles("READER")
                .build();

        when(userManagementStub.registerUser(any(RegisterUserRequest.class))).thenReturn(response);

        UserProfileDto result = client.registerUser("newuser", "securePassw0rd", null, null, TENANT_ID);

        assertThat(result).isNotNull();
        assertThat(result.email()).isNull();
        assertThat(result.displayName()).isNull();
    }

    @Test
    @DisplayName("SWR-059: registerUser propagates ALREADY_EXISTS errors")
    void registerUser_propagatesAlreadyExistsError() {
        when(userManagementStub.registerUser(any(RegisterUserRequest.class)))
                .thenThrow(new StatusRuntimeException(Status.ALREADY_EXISTS.withDescription("Username already taken")));

        assertThatThrownBy(() -> client.registerUser("existing", "securePassw0rd", "a@b.com", "User", TENANT_ID))
                .isInstanceOf(StatusRuntimeException.class);
    }
}
