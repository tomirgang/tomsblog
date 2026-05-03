package de.tomsblog.usermanagement.adapter.inbound.grpc;

import de.tomsblog.grpc.usermanagement.GetTenantSettingsRequest;
import de.tomsblog.grpc.usermanagement.TenantSettingsResponse;
import de.tomsblog.grpc.usermanagement.TenantSettingsServiceGrpc;
import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC server adapter for tenant settings queries.
 * Provides synchronous inter-service communication as per SWR-046.
 *
 * @req SWR-046
 * @req SWR-044
 */
@GrpcService
public class TenantSettingsGrpcService extends TenantSettingsServiceGrpc.TenantSettingsServiceImplBase {

    private final TenantSettingsUseCase tenantSettingsUseCase;

    public TenantSettingsGrpcService(TenantSettingsUseCase tenantSettingsUseCase) {
        this.tenantSettingsUseCase = tenantSettingsUseCase;
    }

    @Override
    public void getTenantSettings(
            GetTenantSettingsRequest request, StreamObserver<TenantSettingsResponse> responseObserver) {
        try {
            TenantSettings settings =
                    tenantSettingsUseCase.getSettings(TenantId.of(UUID.fromString(request.getTenantId())));
            responseObserver.onNext(toResponse(settings));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    private TenantSettingsResponse toResponse(TenantSettings settings) {
        var builder = TenantSettingsResponse.newBuilder()
                .setTenantId(settings.getTenantId().value().toString())
                .setLoginMode(settings.getLoginMode().name())
                .setAutoApproveOidc(settings.isAutoApproveOidc());

        settings.getAutoApproveEmailDomains().forEach(builder::addAutoApproveEmailDomains);

        return builder.build();
    }
}
