package de.tomsblog.tenantmanagement.adapter.inbound.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tomsblog.grpc.tenantmanagement.CreateTenantRequest;
import de.tomsblog.grpc.tenantmanagement.GetTenantRequest;
import de.tomsblog.grpc.tenantmanagement.ListTenantsRequest;
import de.tomsblog.grpc.tenantmanagement.ListTenantsResponse;
import de.tomsblog.grpc.tenantmanagement.TenantResponse;
import de.tomsblog.grpc.tenantmanagement.UpdateGeneralSettingsRequest;
import de.tomsblog.grpc.tenantmanagement.UpdateLegalSettingsRequest;
import de.tomsblog.grpc.tenantmanagement.UpdateOidcSettingsRequest;
import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.tenantmanagement.application.port.inbound.TenantManagementUseCase;
import de.tomsblog.tenantmanagement.domain.model.LoginMode;
import de.tomsblog.tenantmanagement.domain.model.Tenant;
import de.tomsblog.tenantmanagement.domain.model.TenantStatus;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TenantManagementGrpcServiceTest {

    private TenantManagementUseCase useCase;
    private TenantManagementGrpcService grpcService;

    @BeforeEach
    void setUp() {
        useCase = mock(TenantManagementUseCase.class);
        grpcService = new TenantManagementGrpcService(useCase);
    }

    @Test
    @DisplayName("SWR-074: getTenant returns tenant response")
    void getTenantSuccess() {
        var tenantId = TenantId.generate();
        var tenant = createFullTenant(tenantId);
        when(useCase.getTenant(tenantId)).thenReturn(tenant);

        var response = new AtomicReference<TenantResponse>();
        grpcService.getTenant(
                GetTenantRequest.newBuilder()
                        .setTenantId(tenantId.value().toString())
                        .build(),
                captureResponse(response));

        assertThat(response.get()).isNotNull();
        assertThat(response.get().getSlug()).isEqualTo("test");
        assertThat(response.get().getDisplayName()).isEqualTo("Test Blog");
        assertThat(response.get().getStatus()).isEqualTo("ACTIVE");
        assertThat(response.get().getLoginMode()).isEqualTo("BOTH");
    }

    @Test
    @DisplayName("SWR-074: getTenant returns error for invalid tenant")
    void getTenantInvalidId() {
        when(useCase.getTenant(any())).thenThrow(new IllegalArgumentException("not found"));

        var error = new AtomicReference<Throwable>();
        grpcService.getTenant(
                GetTenantRequest.newBuilder()
                        .setTenantId(UUID.randomUUID().toString())
                        .build(),
                captureError(error));

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
    }

    @Test
    @DisplayName("SWR-074: createTenant creates and returns tenant")
    void createTenantSuccess() {
        var tenantId = TenantId.generate();
        when(useCase.createTenant("blog", "My Blog")).thenReturn(Tenant.create(tenantId, "blog", "My Blog"));

        var response = new AtomicReference<TenantResponse>();
        grpcService.createTenant(
                CreateTenantRequest.newBuilder()
                        .setSlug("blog")
                        .setDisplayName("My Blog")
                        .build(),
                captureResponse(response));

        assertThat(response.get().getSlug()).isEqualTo("blog");
        verify(useCase).createTenant("blog", "My Blog");
    }

    @Test
    @DisplayName("SWR-074: createTenant returns error for duplicate")
    void createTenantError() {
        when(useCase.createTenant(any(), any())).thenThrow(new IllegalArgumentException("duplicate"));

        var error = new AtomicReference<Throwable>();
        grpcService.createTenant(
                CreateTenantRequest.newBuilder()
                        .setSlug("blog")
                        .setDisplayName("My Blog")
                        .build(),
                captureError(error));

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
    }

    @Test
    @DisplayName("SWR-074: updateGeneralSettings updates and returns")
    void updateGeneralSettingsSuccess() {
        var tenantId = TenantId.generate();
        var tenant = createFullTenant(tenantId);
        when(useCase.updateGeneralSettings(any(), any(), any(), any(), any(boolean.class), any()))
                .thenReturn(tenant);

        var response = new AtomicReference<TenantResponse>();
        grpcService.updateGeneralSettings(
                UpdateGeneralSettingsRequest.newBuilder()
                        .setTenantId(tenantId.value().toString())
                        .setDisplayName("New Name")
                        .setLoginMode("BOTH")
                        .build(),
                captureResponse(response));

        assertThat(response.get()).isNotNull();
        verify(useCase).updateGeneralSettings(any(), any(), any(), any(), any(boolean.class), any());
    }

    @Test
    @DisplayName("SWR-074: updateOidcSettings updates and returns")
    void updateOidcSettingsSuccess() {
        var tenantId = TenantId.generate();
        var tenant = createFullTenant(tenantId);
        when(useCase.updateOidcSettings(any(), any(), any(), any())).thenReturn(tenant);

        var response = new AtomicReference<TenantResponse>();
        grpcService.updateOidcSettings(
                UpdateOidcSettingsRequest.newBuilder()
                        .setTenantId(tenantId.value().toString())
                        .setOidcIssuerUrl("https://issuer")
                        .setOidcClientId("cid")
                        .setOidcClientSecret("cs")
                        .build(),
                captureResponse(response));

        assertThat(response.get()).isNotNull();
    }

    @Test
    @DisplayName("SWR-074: updateOidcSettings returns error for invalid")
    void updateOidcSettingsError() {
        when(useCase.updateOidcSettings(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("bad oidc"));

        var error = new AtomicReference<Throwable>();
        grpcService.updateOidcSettings(
                UpdateOidcSettingsRequest.newBuilder()
                        .setTenantId(UUID.randomUUID().toString())
                        .build(),
                captureError(error));

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
    }

    @Test
    @DisplayName("SWR-074: updateLegalSettings updates and returns")
    void updateLegalSettingsSuccess() {
        var tenantId = TenantId.generate();
        var tenant = createFullTenant(tenantId);
        when(useCase.updateLegalSettings(any(), any(), any())).thenReturn(tenant);

        var response = new AtomicReference<TenantResponse>();
        grpcService.updateLegalSettings(
                UpdateLegalSettingsRequest.newBuilder()
                        .setTenantId(tenantId.value().toString())
                        .setImpressumContent("imp")
                        .setPrivacyPolicyContent("priv")
                        .build(),
                captureResponse(response));

        assertThat(response.get()).isNotNull();
    }

    @Test
    @DisplayName("SWR-074: updateLegalSettings returns error for invalid")
    void updateLegalSettingsError() {
        when(useCase.updateLegalSettings(any(), any(), any())).thenThrow(new IllegalArgumentException("bad legal"));

        var error = new AtomicReference<Throwable>();
        grpcService.updateLegalSettings(
                UpdateLegalSettingsRequest.newBuilder()
                        .setTenantId(UUID.randomUUID().toString())
                        .build(),
                captureError(error));

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
    }

    @Test
    @DisplayName("SWR-074: updateGeneralSettings returns error for invalid")
    void updateGeneralSettingsError() {
        when(useCase.updateGeneralSettings(any(), any(), any(), any(), any(boolean.class), any()))
                .thenThrow(new IllegalArgumentException("bad general"));

        var error = new AtomicReference<Throwable>();
        grpcService.updateGeneralSettings(
                UpdateGeneralSettingsRequest.newBuilder()
                        .setTenantId(UUID.randomUUID().toString())
                        .setDisplayName("Name")
                        .setLoginMode("INTERNAL")
                        .build(),
                captureError(error));

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
    }

    @Test
    @DisplayName("SWR-074: listTenants returns all tenants")
    void listTenantsSuccess() {
        var t1 = Tenant.create(TenantId.generate(), "a", "A");
        var t2 = Tenant.create(TenantId.generate(), "b", "B");
        when(useCase.listAllTenants()).thenReturn(List.of(t1, t2));

        var response = new AtomicReference<ListTenantsResponse>();
        grpcService.listTenants(ListTenantsRequest.newBuilder().build(), new StreamObserver<>() {
            @Override
            public void onNext(ListTenantsResponse value) {
                response.set(value);
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {}
        });

        assertThat(response.get().getTenantsList()).hasSize(2);
        assertThat(response.get().getTenants(0).getSlug()).isEqualTo("a");
    }

    @Test
    @DisplayName("SWR-074: updateGeneralSettings passes null for empty tagline")
    void updateGeneralSettingsEmptyTagline() {
        var tenantId = TenantId.generate();
        var tenant = createFullTenant(tenantId);
        when(useCase.updateGeneralSettings(any(), any(), any(), any(), any(boolean.class), any()))
                .thenReturn(tenant);

        var response = new AtomicReference<TenantResponse>();
        grpcService.updateGeneralSettings(
                UpdateGeneralSettingsRequest.newBuilder()
                        .setTenantId(tenantId.value().toString())
                        .setDisplayName("Name")
                        .setTagline("")
                        .setLoginMode("INTERNAL")
                        .build(),
                captureResponse(response));

        verify(useCase)
                .updateGeneralSettings(
                        any(), any(), org.mockito.ArgumentMatchers.isNull(), any(), any(boolean.class), any());
    }

    @Test
    @DisplayName("SWR-074: updateGeneralSettings passes non-empty tagline through")
    void updateGeneralSettingsNonEmptyTagline() {
        var tenantId = TenantId.generate();
        var tenant = createFullTenant(tenantId);
        when(useCase.updateGeneralSettings(any(), any(), any(), any(), any(boolean.class), any()))
                .thenReturn(tenant);

        var response = new AtomicReference<TenantResponse>();
        grpcService.updateGeneralSettings(
                UpdateGeneralSettingsRequest.newBuilder()
                        .setTenantId(tenantId.value().toString())
                        .setDisplayName("Name")
                        .setTagline("My tagline")
                        .setLoginMode("INTERNAL")
                        .build(),
                captureResponse(response));

        verify(useCase)
                .updateGeneralSettings(
                        any(), any(), org.mockito.ArgumentMatchers.eq("My tagline"), any(), any(boolean.class), any());
    }

    private Tenant createFullTenant(TenantId tenantId) {
        return Tenant.reconstitute(
                tenantId,
                "test",
                "Test Blog",
                "A tag",
                TenantStatus.ACTIVE,
                LoginMode.BOTH,
                false,
                Set.of("example.com"),
                "imp",
                "priv",
                "https://issuer",
                "cid",
                "cs");
    }

    @SuppressWarnings("unchecked")
    private StreamObserver<TenantResponse> captureResponse(AtomicReference<TenantResponse> ref) {
        return new StreamObserver<>() {
            @Override
            public void onNext(TenantResponse value) {
                ref.set(value);
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {}
        };
    }

    @SuppressWarnings("unchecked")
    private StreamObserver<TenantResponse> captureError(AtomicReference<Throwable> ref) {
        return new StreamObserver<>() {
            @Override
            public void onNext(TenantResponse value) {}

            @Override
            public void onError(Throwable t) {
                ref.set(t);
            }

            @Override
            public void onCompleted() {}
        };
    }
}
