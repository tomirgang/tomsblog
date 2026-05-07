package de.tomsblog.usermanagement.adapter.inbound.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tomsblog.grpc.usermanagement.ApproveUserRequest;
import de.tomsblog.grpc.usermanagement.ChangeUserRoleRequest;
import de.tomsblog.grpc.usermanagement.FindByOidcSubjectRequest;
import de.tomsblog.grpc.usermanagement.FindByUsernameRequest;
import de.tomsblog.grpc.usermanagement.ListUsersByTenantRequest;
import de.tomsblog.grpc.usermanagement.ListUsersResponse;
import de.tomsblog.grpc.usermanagement.RegisterUserRequest;
import de.tomsblog.grpc.usermanagement.RejectUserRequest;
import de.tomsblog.grpc.usermanagement.SyncOidcUserRequest;
import de.tomsblog.grpc.usermanagement.UserProfileResponse;
import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.RegisterUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.SyncOidcUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.UserProfileUseCase;
import de.tomsblog.usermanagement.application.service.UserAlreadyExistsException;
import de.tomsblog.usermanagement.application.service.UserProfileNotFoundException;
import de.tomsblog.usermanagement.domain.model.Role;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link UserManagementGrpcService}.
 *
 * @req SWR-046
 * @req SWR-043
 */
@ExtendWith(MockitoExtension.class)
class UserManagementGrpcServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private UserProfileUseCase userProfileUseCase;

    private UserManagementGrpcService grpcService;

    @BeforeEach
    void setUp() {
        grpcService = new UserManagementGrpcService(userProfileUseCase);
    }

    @Test
    @DisplayName("SWR-046: syncOidcUser returns profile via gRPC")
    void syncOidcUser_returnsProfile() {
        var profile = UserProfile.createFromOidc("sub-123", "user@test.com", "Test User");
        profile.addTenantMembership(new de.tomsblog.usermanagement.domain.model.TenantMembership(
                TenantId.of(TENANT_ID), de.tomsblog.usermanagement.domain.model.Role.AUTHOR));
        when(userProfileUseCase.syncFromOidc(any(SyncOidcUserCommand.class))).thenReturn(profile);

        var request = SyncOidcUserRequest.newBuilder()
                .setOidcSubject("sub-123")
                .setEmail("user@test.com")
                .setDisplayName("Test User")
                .addOidcGroups("editors")
                .setTenantId(TENANT_ID.toString())
                .build();

        var result = new AtomicReference<UserProfileResponse>();
        var observer = createObserver(result);

        grpcService.syncOidcUser(request, observer);

        assertThat(result.get()).isNotNull();
        assertThat(result.get().getOidcSubject()).isEqualTo("sub-123");
        assertThat(result.get().getEmail()).isEqualTo("user@test.com");
        assertThat(result.get().getAuthSource()).isEqualTo("OIDC");
        verify(userProfileUseCase).syncFromOidc(any(SyncOidcUserCommand.class));
    }

    @Test
    @DisplayName("SWR-046: syncOidcUser returns INVALID_ARGUMENT on bad input")
    void syncOidcUser_invalidArgument() {
        var request = SyncOidcUserRequest.newBuilder()
                .setOidcSubject("")
                .setTenantId(TENANT_ID.toString())
                .build();

        var error = new AtomicReference<Throwable>();
        StreamObserver<UserProfileResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(UserProfileResponse value) {}

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }

            @Override
            public void onCompleted() {}
        };

        grpcService.syncOidcUser(request, observer);

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode())
                .isEqualTo(Status.INVALID_ARGUMENT.getCode());
    }

    @Test
    @DisplayName("SWR-046: findByUsername returns profile via gRPC")
    void findByUsername_returnsProfile() {
        var profile = UserProfile.createInternal("admin", "hash", "admin@test.com", "Admin");
        when(userProfileUseCase.findByUsername("admin")).thenReturn(profile);

        var request = FindByUsernameRequest.newBuilder().setUsername("admin").build();

        var result = new AtomicReference<UserProfileResponse>();
        var observer = createObserver(result);

        grpcService.findByUsername(request, observer);

        assertThat(result.get()).isNotNull();
        assertThat(result.get().getUsername()).isEqualTo("admin");
        assertThat(result.get().getAuthSource()).isEqualTo("INTERNAL");
    }

    @Test
    @DisplayName("SWR-046: findByUsername returns NOT_FOUND when user does not exist")
    void findByUsername_notFound() {
        when(userProfileUseCase.findByUsername("unknown")).thenThrow(new UserProfileNotFoundException("unknown"));

        var request = FindByUsernameRequest.newBuilder().setUsername("unknown").build();

        var error = new AtomicReference<Throwable>();
        StreamObserver<UserProfileResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(UserProfileResponse value) {}

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }

            @Override
            public void onCompleted() {}
        };

        grpcService.findByUsername(request, observer);

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("SWR-046: findByOidcSubject returns profile via gRPC")
    void findByOidcSubject_returnsProfile() {
        var profile = UserProfile.createFromOidc("oidc-sub-xyz", "xyz@test.com", "XYZ User");
        when(userProfileUseCase.findByOidcSubject("oidc-sub-xyz")).thenReturn(profile);

        var request = FindByOidcSubjectRequest.newBuilder()
                .setOidcSubject("oidc-sub-xyz")
                .build();

        var result = new AtomicReference<UserProfileResponse>();
        var observer = createObserver(result);

        grpcService.findByOidcSubject(request, observer);

        assertThat(result.get()).isNotNull();
        assertThat(result.get().getOidcSubject()).isEqualTo("oidc-sub-xyz");
        assertThat(result.get().getEmail()).isEqualTo("xyz@test.com");
    }

    private StreamObserver<UserProfileResponse> createObserver(AtomicReference<UserProfileResponse> result) {
        return new StreamObserver<>() {
            @Override
            public void onNext(UserProfileResponse value) {
                result.set(value);
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {}
        };
    }

    @Test
    @DisplayName("SWR-051: listUsersByTenant returns users for tenant via gRPC")
    void listUsersByTenant_returnsUsers() {
        var profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User1");
        when(userProfileUseCase.listByTenantId(any(TenantId.class))).thenReturn(List.of(profile));

        var request = ListUsersByTenantRequest.newBuilder()
                .setTenantId(TENANT_ID.toString())
                .build();

        var result = new AtomicReference<ListUsersResponse>();
        StreamObserver<ListUsersResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(ListUsersResponse value) {
                result.set(value);
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {}
        };

        grpcService.listUsersByTenant(request, observer);

        assertThat(result.get().getUsersList()).hasSize(1);
        assertThat(result.get().getUsers(0).getOidcSubject()).isEqualTo("sub-1");
    }

    @Test
    @DisplayName("SWR-051: listUsersByTenant returns INVALID_ARGUMENT on bad tenant ID")
    void listUsersByTenant_invalidTenantId() {
        var request = ListUsersByTenantRequest.newBuilder()
                .setTenantId("invalid-uuid")
                .build();

        var error = new AtomicReference<Throwable>();
        StreamObserver<ListUsersResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(ListUsersResponse value) {}

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }

            @Override
            public void onCompleted() {}
        };

        grpcService.listUsersByTenant(request, observer);

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode())
                .isEqualTo(Status.INVALID_ARGUMENT.getCode());
    }

    @Test
    @DisplayName("SWR-051: approveUser approves and returns profile via gRPC")
    void approveUser_approvesAndReturns() {
        var profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User1");
        when(userProfileUseCase.findByOidcSubject("sub-1")).thenReturn(profile);

        var request = ApproveUserRequest.newBuilder().setIdentifier("sub-1").build();

        var result = new AtomicReference<UserProfileResponse>();
        var observer = createObserver(result);

        grpcService.approveUser(request, observer);

        verify(userProfileUseCase).approveUser("sub-1");
        assertThat(result.get()).isNotNull();
        assertThat(result.get().getOidcSubject()).isEqualTo("sub-1");
    }

    @Test
    @DisplayName("SWR-051: approveUser returns NOT_FOUND when user missing")
    void approveUser_notFound() {
        doThrow(new UserProfileNotFoundException("sub-x"))
                .when(userProfileUseCase)
                .approveUser("sub-x");

        var request = ApproveUserRequest.newBuilder().setIdentifier("sub-x").build();

        var error = new AtomicReference<Throwable>();
        StreamObserver<UserProfileResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(UserProfileResponse value) {}

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }

            @Override
            public void onCompleted() {}
        };

        grpcService.approveUser(request, observer);

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("SWR-051: rejectUser rejects and returns profile via gRPC")
    void rejectUser_rejectsAndReturns() {
        var profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User1");
        when(userProfileUseCase.findByOidcSubject("sub-1")).thenReturn(profile);

        var request = RejectUserRequest.newBuilder().setIdentifier("sub-1").build();

        var result = new AtomicReference<UserProfileResponse>();
        var observer = createObserver(result);

        grpcService.rejectUser(request, observer);

        verify(userProfileUseCase).rejectUser("sub-1");
        assertThat(result.get()).isNotNull();
    }

    @Test
    @DisplayName("SWR-051: rejectUser returns NOT_FOUND when user missing")
    void rejectUser_notFound() {
        doThrow(new UserProfileNotFoundException("sub-x"))
                .when(userProfileUseCase)
                .rejectUser("sub-x");

        var request = RejectUserRequest.newBuilder().setIdentifier("sub-x").build();

        var error = new AtomicReference<Throwable>();
        StreamObserver<UserProfileResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(UserProfileResponse value) {}

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }

            @Override
            public void onCompleted() {}
        };

        grpcService.rejectUser(request, observer);

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("SWR-051: changeUserRole changes role and returns profile via gRPC")
    void changeUserRole_changesAndReturns() {
        var profile = UserProfile.createFromOidc("sub-1", "a@b.com", "User1");
        when(userProfileUseCase.findByOidcSubject("sub-1")).thenReturn(profile);

        var request = ChangeUserRoleRequest.newBuilder()
                .setIdentifier("sub-1")
                .setTenantId(TENANT_ID.toString())
                .setRole("ADMIN")
                .build();

        var result = new AtomicReference<UserProfileResponse>();
        var observer = createObserver(result);

        grpcService.changeUserRole(request, observer);

        verify(userProfileUseCase).removeTenantMembership("sub-1", TenantId.of(TENANT_ID));
        verify(userProfileUseCase).addTenantMembership("sub-1", TenantId.of(TENANT_ID), Role.ADMIN);
        assertThat(result.get()).isNotNull();
    }

    @Test
    @DisplayName("SWR-051: changeUserRole returns INVALID_ARGUMENT on bad role")
    void changeUserRole_invalidRole() {
        var request = ChangeUserRoleRequest.newBuilder()
                .setIdentifier("sub-1")
                .setTenantId(TENANT_ID.toString())
                .setRole("INVALID_ROLE")
                .build();

        var error = new AtomicReference<Throwable>();
        StreamObserver<UserProfileResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(UserProfileResponse value) {}

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }

            @Override
            public void onCompleted() {}
        };

        grpcService.changeUserRole(request, observer);

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode())
                .isEqualTo(Status.INVALID_ARGUMENT.getCode());
    }

    @Test
    @DisplayName("SWR-046: findByOidcSubject returns NOT_FOUND when not found")
    void findByOidcSubject_notFound() {
        when(userProfileUseCase.findByOidcSubject("unknown")).thenThrow(new UserProfileNotFoundException("unknown"));

        var request =
                FindByOidcSubjectRequest.newBuilder().setOidcSubject("unknown").build();

        var error = new AtomicReference<Throwable>();
        StreamObserver<UserProfileResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(UserProfileResponse value) {}

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }

            @Override
            public void onCompleted() {}
        };

        grpcService.findByOidcSubject(request, observer);

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("SWR-051: approveUser falls back to username when oidc subject not found")
    void approveUser_fallsBackToUsername() {
        var profile = UserProfile.createInternal("admin", "hash", "admin@test.com", "Admin");
        when(userProfileUseCase.findByOidcSubject("admin")).thenThrow(new UserProfileNotFoundException("admin"));
        when(userProfileUseCase.findByUsername("admin")).thenReturn(profile);

        var request = ApproveUserRequest.newBuilder().setIdentifier("admin").build();

        var result = new AtomicReference<UserProfileResponse>();
        var observer = createObserver(result);

        grpcService.approveUser(request, observer);

        verify(userProfileUseCase).approveUser("admin");
        assertThat(result.get().getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("SWR-046: findByUsername returns INVALID_ARGUMENT on IllegalArgumentException")
    void findByUsername_invalidArgument() {
        when(userProfileUseCase.findByUsername("")).thenThrow(new IllegalArgumentException("username empty"));

        var request = FindByUsernameRequest.newBuilder().setUsername("").build();

        var error = new AtomicReference<Throwable>();
        StreamObserver<UserProfileResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(UserProfileResponse value) {}

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }

            @Override
            public void onCompleted() {}
        };

        grpcService.findByUsername(request, observer);

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode())
                .isEqualTo(Status.INVALID_ARGUMENT.getCode());
    }

    @Test
    @DisplayName("SWR-046: findByOidcSubject returns INVALID_ARGUMENT on IllegalArgumentException")
    void findByOidcSubject_invalidArgument() {
        when(userProfileUseCase.findByOidcSubject("")).thenThrow(new IllegalArgumentException("subject empty"));

        var request = FindByOidcSubjectRequest.newBuilder().setOidcSubject("").build();

        var error = new AtomicReference<Throwable>();
        StreamObserver<UserProfileResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(UserProfileResponse value) {}

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }

            @Override
            public void onCompleted() {}
        };

        grpcService.findByOidcSubject(request, observer);

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode())
                .isEqualTo(Status.INVALID_ARGUMENT.getCode());
    }

    @Test
    @DisplayName("SWR-051: changeUserRole returns NOT_FOUND when user not found")
    void changeUserRole_notFound() {
        doThrow(new UserProfileNotFoundException("sub-x"))
                .when(userProfileUseCase)
                .removeTenantMembership("sub-x", TenantId.of(TENANT_ID));

        var request = ChangeUserRoleRequest.newBuilder()
                .setIdentifier("sub-x")
                .setTenantId(TENANT_ID.toString())
                .setRole("AUTHOR")
                .build();

        var error = new AtomicReference<Throwable>();
        StreamObserver<UserProfileResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(UserProfileResponse value) {}

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }

            @Override
            public void onCompleted() {}
        };

        grpcService.changeUserRole(request, observer);

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("SWR-046: toResponse handles null email and displayName")
    void toResponse_handlesNullFields() {
        var profile = UserProfile.createFromOidc("sub-null", null, null);
        when(userProfileUseCase.findByOidcSubject("sub-null")).thenReturn(profile);

        var request =
                FindByOidcSubjectRequest.newBuilder().setOidcSubject("sub-null").build();

        var result = new AtomicReference<UserProfileResponse>();
        var observer = createObserver(result);

        grpcService.findByOidcSubject(request, observer);

        assertThat(result.get()).isNotNull();
        assertThat(result.get().getOidcSubject()).isEqualTo("sub-null");
        assertThat(result.get().getEmail()).isEmpty();
        assertThat(result.get().getDisplayName()).isEmpty();
    }

    @Test
    @DisplayName("SWR-059: registerUser returns profile via gRPC")
    void registerUser_returnsProfile() {
        var profile = UserProfile.createInternal("newuser", "hashed", "new@test.com", "New User");
        when(userProfileUseCase.register(any(RegisterUserCommand.class))).thenReturn(profile);

        var request = RegisterUserRequest.newBuilder()
                .setUsername("newuser")
                .setPassword("securePassw0rd")
                .setEmail("new@test.com")
                .setDisplayName("New User")
                .setTenantId(TENANT_ID.toString())
                .build();

        var result = new AtomicReference<UserProfileResponse>();
        var observer = createObserver(result);

        grpcService.registerUser(request, observer);

        assertThat(result.get()).isNotNull();
        assertThat(result.get().getUsername()).isEqualTo("newuser");
        assertThat(result.get().getEmail()).isEqualTo("new@test.com");
    }

    @Test
    @DisplayName("SWR-059: registerUser with empty displayName passes null")
    void registerUser_emptyDisplayNamePassesNull() {
        var profile = UserProfile.createInternal("newuser", "hashed", "new@test.com", null);
        when(userProfileUseCase.register(any(RegisterUserCommand.class))).thenReturn(profile);

        var request = RegisterUserRequest.newBuilder()
                .setUsername("newuser")
                .setPassword("securePassw0rd")
                .setEmail("new@test.com")
                .setTenantId(TENANT_ID.toString())
                .build();

        var result = new AtomicReference<UserProfileResponse>();
        var observer = createObserver(result);

        grpcService.registerUser(request, observer);

        assertThat(result.get()).isNotNull();
        assertThat(result.get().getDisplayName()).isEmpty();
    }

    @Test
    @DisplayName("SWR-059: registerUser returns ALREADY_EXISTS when user exists")
    void registerUser_alreadyExists() {
        when(userProfileUseCase.register(any(RegisterUserCommand.class)))
                .thenThrow(new UserAlreadyExistsException("Username already taken"));

        var request = RegisterUserRequest.newBuilder()
                .setUsername("existing")
                .setPassword("securePassw0rd")
                .setEmail("ex@test.com")
                .setTenantId(TENANT_ID.toString())
                .build();

        var error = new AtomicReference<Throwable>();
        var observer = new StreamObserver<UserProfileResponse>() {
            @Override
            public void onNext(UserProfileResponse value) {}

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }

            @Override
            public void onCompleted() {}
        };

        grpcService.registerUser(request, observer);

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode())
                .isEqualTo(Status.ALREADY_EXISTS.getCode());
    }

    @Test
    @DisplayName("SWR-059: registerUser returns INVALID_ARGUMENT on invalid input")
    void registerUser_invalidArgument() {
        var request = RegisterUserRequest.newBuilder()
                .setUsername("")
                .setPassword("securePassw0rd")
                .setEmail("a@b.com")
                .setTenantId(TENANT_ID.toString())
                .build();

        var error = new AtomicReference<Throwable>();
        var observer = new StreamObserver<UserProfileResponse>() {
            @Override
            public void onNext(UserProfileResponse value) {}

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }

            @Override
            public void onCompleted() {}
        };

        grpcService.registerUser(request, observer);

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode())
                .isEqualTo(Status.INVALID_ARGUMENT.getCode());
    }
}
