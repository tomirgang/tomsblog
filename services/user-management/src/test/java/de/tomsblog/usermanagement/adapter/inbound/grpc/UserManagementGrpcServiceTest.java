package de.tomsblog.usermanagement.adapter.inbound.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tomsblog.grpc.usermanagement.FindByOidcSubjectRequest;
import de.tomsblog.grpc.usermanagement.FindByUsernameRequest;
import de.tomsblog.grpc.usermanagement.SyncOidcUserRequest;
import de.tomsblog.grpc.usermanagement.UserProfileResponse;
import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.SyncOidcUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.UserProfileUseCase;
import de.tomsblog.usermanagement.application.service.UserProfileNotFoundException;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
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
}
