package de.tomsblog.usermanagement.adapter.inbound.grpc;

import de.tomsblog.grpc.usermanagement.GetTenantSettingsRequest;
import de.tomsblog.grpc.usermanagement.ListTenantsRequest;
import de.tomsblog.grpc.usermanagement.ListTenantsResponse;
import de.tomsblog.grpc.usermanagement.TenantInfoResponse;
import de.tomsblog.grpc.usermanagement.TenantSettingsResponse;
import de.tomsblog.grpc.usermanagement.TenantSettingsServiceGrpc;
import de.tomsblog.grpc.usermanagement.UpdateTenantSettingsRequest;
import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.HashSet;
import java.util.UUID;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC server adapter for tenant settings queries and updates.
 * Provides synchronous inter-service communication as per SWR-046.
 *
 * @req SWR-046
 * @req SWR-044
 * @req SWR-050
 * @req SWR-052
 * @req SWR-053
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

    @Override
    public void updateTenantSettings(
            UpdateTenantSettingsRequest request, StreamObserver<TenantSettingsResponse> responseObserver) {
        try {
            TenantId tenantId = TenantId.of(UUID.fromString(request.getTenantId()));
            LoginMode loginMode = LoginMode.valueOf(request.getLoginMode());
            var settings = tenantSettingsUseCase.updateSettings(
                    tenantId,
                    loginMode,
                    request.getAutoApproveOidc(),
                    new HashSet<>(request.getAutoApproveEmailDomainsList()),
                    request.getDisplayName(),
                    request.getTagline().isEmpty() ? null : request.getTagline(),
                    request.getImpressumContent().isEmpty() ? null : request.getImpressumContent(),
                    request.getPrivacyPolicyContent().isEmpty() ? null : request.getPrivacyPolicyContent());
            responseObserver.onNext(toResponse(settings));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void listTenants(ListTenantsRequest request, StreamObserver<ListTenantsResponse> responseObserver) {
        var tenants = tenantSettingsUseCase.listAllTenants();
        var builder = ListTenantsResponse.newBuilder();
        for (TenantSettings t : tenants) {
            builder.addTenants(TenantInfoResponse.newBuilder()
                    .setTenantId(t.getTenantId().value().toString())
                    .setDisplayName(t.getDisplayName() != null ? t.getDisplayName() : "")
                    .build());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private TenantSettingsResponse toResponse(TenantSettings settings) {
        var builder = TenantSettingsResponse.newBuilder()
                .setTenantId(settings.getTenantId().value().toString())
                .setLoginMode(settings.getLoginMode().name())
                .setAutoApproveOidc(settings.isAutoApproveOidc())
                .setDisplayName(settings.getDisplayName() != null ? settings.getDisplayName() : "Toms Blog")
                .setTagline(settings.getTagline() != null ? settings.getTagline() : "")
                .setImpressumContent(settings.getImpressumContent() != null ? settings.getImpressumContent() : "")
                .setPrivacyPolicyContent(
                        settings.getPrivacyPolicyContent() != null ? settings.getPrivacyPolicyContent() : "");

        settings.getAutoApproveEmailDomains().forEach(builder::addAutoApproveEmailDomains);

        return builder.build();
    }
}
