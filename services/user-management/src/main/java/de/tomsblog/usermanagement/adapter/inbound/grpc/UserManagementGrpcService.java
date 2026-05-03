package de.tomsblog.usermanagement.adapter.inbound.grpc;

import de.tomsblog.grpc.usermanagement.FindByOidcSubjectRequest;
import de.tomsblog.grpc.usermanagement.FindByUsernameRequest;
import de.tomsblog.grpc.usermanagement.SyncOidcUserRequest;
import de.tomsblog.grpc.usermanagement.TenantMembership;
import de.tomsblog.grpc.usermanagement.UserManagementServiceGrpc;
import de.tomsblog.grpc.usermanagement.UserProfileResponse;
import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.SyncOidcUserCommand;
import de.tomsblog.usermanagement.application.port.inbound.UserProfileUseCase;
import de.tomsblog.usermanagement.application.service.UserProfileNotFoundException;
import de.tomsblog.usermanagement.domain.model.UserProfile;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC server adapter for user profile operations.
 * Provides synchronous inter-service communication as per SWR-046.
 *
 * @req SWR-046
 * @req SWR-043
 */
@GrpcService
public class UserManagementGrpcService extends UserManagementServiceGrpc.UserManagementServiceImplBase {

    private final UserProfileUseCase userProfileUseCase;

    public UserManagementGrpcService(UserProfileUseCase userProfileUseCase) {
        this.userProfileUseCase = userProfileUseCase;
    }

    @Override
    public void syncOidcUser(SyncOidcUserRequest request, StreamObserver<UserProfileResponse> responseObserver) {
        try {
            var command = new SyncOidcUserCommand(
                    request.getOidcSubject(),
                    request.getEmail(),
                    request.getDisplayName(),
                    request.getOidcGroupsList(),
                    TenantId.of(UUID.fromString(request.getTenantId())));
            var profile = userProfileUseCase.syncFromOidc(command);
            responseObserver.onNext(toResponse(profile));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void findByUsername(FindByUsernameRequest request, StreamObserver<UserProfileResponse> responseObserver) {
        try {
            var profile = userProfileUseCase.findByUsername(request.getUsername());
            responseObserver.onNext(toResponse(profile));
            responseObserver.onCompleted();
        } catch (UserProfileNotFoundException e) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void findByOidcSubject(
            FindByOidcSubjectRequest request, StreamObserver<UserProfileResponse> responseObserver) {
        try {
            var profile = userProfileUseCase.findByOidcSubject(request.getOidcSubject());
            responseObserver.onNext(toResponse(profile));
            responseObserver.onCompleted();
        } catch (UserProfileNotFoundException e) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        var builder = UserProfileResponse.newBuilder()
                .setId(profile.getId().value().toString())
                .setAuthSource(profile.getAuthSource().name())
                .setApprovalStatus(profile.getApprovalStatus().name());

        if (profile.getOidcSubject() != null) {
            builder.setOidcSubject(profile.getOidcSubject());
        }
        if (profile.getUsername() != null) {
            builder.setUsername(profile.getUsername());
        }
        if (profile.getEmail() != null) {
            builder.setEmail(profile.getEmail());
        }
        if (profile.getDisplayName() != null) {
            builder.setDisplayName(profile.getDisplayName());
        }

        profile.getGlobalRoles().forEach(role -> builder.addGlobalRoles(role.name()));

        profile.getTenantMemberships()
                .forEach(membership -> builder.addTenantMemberships(TenantMembership.newBuilder()
                        .setTenantId(membership.tenantId().value().toString())
                        .setRole(membership.role().name())
                        .build()));

        return builder.build();
    }
}
