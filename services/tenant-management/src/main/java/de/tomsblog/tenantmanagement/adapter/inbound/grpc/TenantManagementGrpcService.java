package de.tomsblog.tenantmanagement.adapter.inbound.grpc;

import de.tomsblog.grpc.tenantmanagement.CreateTenantRequest;
import de.tomsblog.grpc.tenantmanagement.GetTenantRequest;
import de.tomsblog.grpc.tenantmanagement.ListTenantsRequest;
import de.tomsblog.grpc.tenantmanagement.ListTenantsResponse;
import de.tomsblog.grpc.tenantmanagement.TenantInfoResponse;
import de.tomsblog.grpc.tenantmanagement.TenantManagementServiceGrpc;
import de.tomsblog.grpc.tenantmanagement.TenantResponse;
import de.tomsblog.grpc.tenantmanagement.UpdateGeneralSettingsRequest;
import de.tomsblog.grpc.tenantmanagement.UpdateLegalSettingsRequest;
import de.tomsblog.grpc.tenantmanagement.UpdateOidcSettingsRequest;
import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.tenantmanagement.application.port.inbound.TenantManagementUseCase;
import de.tomsblog.tenantmanagement.domain.model.Tenant;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.HashSet;
import java.util.UUID;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC server adapter for tenant management (ADR-0031).
 *
 * @req SWR-074
 */
@GrpcService
public class TenantManagementGrpcService extends TenantManagementServiceGrpc.TenantManagementServiceImplBase {

    private final TenantManagementUseCase tenantManagementUseCase;

    public TenantManagementGrpcService(TenantManagementUseCase tenantManagementUseCase) {
        this.tenantManagementUseCase = tenantManagementUseCase;
    }

    @Override
    public void getTenant(GetTenantRequest request, StreamObserver<TenantResponse> responseObserver) {
        try {
            var tenant = tenantManagementUseCase.getTenant(TenantId.of(UUID.fromString(request.getTenantId())));
            responseObserver.onNext(toResponse(tenant));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void createTenant(CreateTenantRequest request, StreamObserver<TenantResponse> responseObserver) {
        try {
            var tenant = tenantManagementUseCase.createTenant(request.getSlug(), request.getDisplayName());
            responseObserver.onNext(toResponse(tenant));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void updateGeneralSettings(
            UpdateGeneralSettingsRequest request, StreamObserver<TenantResponse> responseObserver) {
        try {
            var tenantId = TenantId.of(UUID.fromString(request.getTenantId()));
            var tenant = tenantManagementUseCase.updateGeneralSettings(
                    tenantId,
                    request.getDisplayName(),
                    request.getTagline().isEmpty() ? null : request.getTagline(),
                    request.getLoginMode(),
                    request.getAutoApproveOidc(),
                    new HashSet<>(request.getAutoApproveEmailDomainsList()));
            responseObserver.onNext(toResponse(tenant));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void updateOidcSettings(UpdateOidcSettingsRequest request, StreamObserver<TenantResponse> responseObserver) {
        try {
            var tenantId = TenantId.of(UUID.fromString(request.getTenantId()));
            var tenant = tenantManagementUseCase.updateOidcSettings(
                    tenantId,
                    request.getOidcIssuerUrl().isEmpty() ? null : request.getOidcIssuerUrl(),
                    request.getOidcClientId().isEmpty() ? null : request.getOidcClientId(),
                    request.getOidcClientSecret().isEmpty() ? null : request.getOidcClientSecret());
            responseObserver.onNext(toResponse(tenant));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void updateLegalSettings(
            UpdateLegalSettingsRequest request, StreamObserver<TenantResponse> responseObserver) {
        try {
            var tenantId = TenantId.of(UUID.fromString(request.getTenantId()));
            var tenant = tenantManagementUseCase.updateLegalSettings(
                    tenantId,
                    request.getImpressumContent().isEmpty() ? null : request.getImpressumContent(),
                    request.getPrivacyPolicyContent().isEmpty() ? null : request.getPrivacyPolicyContent());
            responseObserver.onNext(toResponse(tenant));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void listTenants(ListTenantsRequest request, StreamObserver<ListTenantsResponse> responseObserver) {
        var tenants = tenantManagementUseCase.listAllTenants();
        var builder = ListTenantsResponse.newBuilder();
        for (Tenant t : tenants) {
            builder.addTenants(TenantInfoResponse.newBuilder()
                    .setTenantId(t.getTenantId().value().toString())
                    .setSlug(t.getSlug())
                    .setDisplayName(t.getDisplayName())
                    .setStatus(t.getStatus().name())
                    .build());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private TenantResponse toResponse(Tenant tenant) {
        var builder = TenantResponse.newBuilder()
                .setTenantId(tenant.getTenantId().value().toString())
                .setSlug(tenant.getSlug())
                .setDisplayName(tenant.getDisplayName())
                .setTagline(tenant.getTagline() != null ? tenant.getTagline() : "")
                .setStatus(tenant.getStatus().name())
                .setLoginMode(tenant.getLoginMode().name())
                .setAutoApproveOidc(tenant.isAutoApproveOidc())
                .setImpressumContent(tenant.getImpressumContent() != null ? tenant.getImpressumContent() : "")
                .setPrivacyPolicyContent(
                        tenant.getPrivacyPolicyContent() != null ? tenant.getPrivacyPolicyContent() : "")
                .setOidcIssuerUrl(tenant.getOidcIssuerUrl() != null ? tenant.getOidcIssuerUrl() : "")
                .setOidcClientId(tenant.getOidcClientId() != null ? tenant.getOidcClientId() : "")
                .setOidcClientSecret(tenant.getOidcClientSecret() != null ? tenant.getOidcClientSecret() : "");

        tenant.getAutoApproveEmailDomains().forEach(builder::addAutoApproveEmailDomains);

        return builder.build();
    }
}
