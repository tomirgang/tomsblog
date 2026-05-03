package de.tomsblog.usermanagement.adapter.inbound.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import de.tomsblog.grpc.usermanagement.GetTenantSettingsRequest;
import de.tomsblog.grpc.usermanagement.ListTenantsRequest;
import de.tomsblog.grpc.usermanagement.ListTenantsResponse;
import de.tomsblog.grpc.usermanagement.TenantSettingsResponse;
import de.tomsblog.grpc.usermanagement.UpdateTenantSettingsRequest;
import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.domain.model.LoginMode;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TenantSettingsGrpcService}.
 *
 * @req SWR-046
 * @req SWR-044
 */
@ExtendWith(MockitoExtension.class)
class TenantSettingsGrpcServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private TenantSettingsUseCase tenantSettingsUseCase;

    private TenantSettingsGrpcService grpcService;

    @BeforeEach
    void setUp() {
        grpcService = new TenantSettingsGrpcService(tenantSettingsUseCase);
    }

    @Test
    @DisplayName("SWR-046: getTenantSettings returns settings via gRPC")
    void getTenantSettings_returnsSettings() {
        var settings = TenantSettings.create(TenantId.of(TENANT_ID));
        when(tenantSettingsUseCase.getSettings(any(TenantId.class))).thenReturn(settings);

        var request = GetTenantSettingsRequest.newBuilder()
                .setTenantId(TENANT_ID.toString())
                .build();

        var result = new AtomicReference<TenantSettingsResponse>();
        StreamObserver<TenantSettingsResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(TenantSettingsResponse value) {
                result.set(value);
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {}
        };

        grpcService.getTenantSettings(request, observer);

        assertThat(result.get()).isNotNull();
        assertThat(result.get().getTenantId()).isEqualTo(TENANT_ID.toString());
        assertThat(result.get().getLoginMode()).isEqualTo("BOTH");
    }

    @Test
    @DisplayName("SWR-046: getTenantSettings returns INVALID_ARGUMENT on bad tenant ID")
    void getTenantSettings_invalidArgument() {
        var request =
                GetTenantSettingsRequest.newBuilder().setTenantId("not-a-uuid").build();

        var error = new AtomicReference<Throwable>();
        StreamObserver<TenantSettingsResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(TenantSettingsResponse value) {}

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }

            @Override
            public void onCompleted() {}
        };

        grpcService.getTenantSettings(request, observer);

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode())
                .isEqualTo(Status.INVALID_ARGUMENT.getCode());
    }

    @Test
    @DisplayName("SWR-052: updateTenantSettings updates and returns settings via gRPC")
    void updateTenantSettings_updatesAndReturns() {
        var settings = TenantSettings.reconstitute(
                TenantId.of(TENANT_ID), LoginMode.OIDC, true, Set.of("test.com"), "My Blog", "A tagline");
        when(tenantSettingsUseCase.updateSettings(
                        eq(TenantId.of(TENANT_ID)),
                        eq(LoginMode.OIDC),
                        eq(true),
                        eq(Set.of("test.com")),
                        eq("My Blog"),
                        eq("A tagline")))
                .thenReturn(settings);

        var request = UpdateTenantSettingsRequest.newBuilder()
                .setTenantId(TENANT_ID.toString())
                .setLoginMode("OIDC")
                .setAutoApproveOidc(true)
                .addAutoApproveEmailDomains("test.com")
                .setDisplayName("My Blog")
                .setTagline("A tagline")
                .build();

        var result = new AtomicReference<TenantSettingsResponse>();
        StreamObserver<TenantSettingsResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(TenantSettingsResponse value) {
                result.set(value);
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {}
        };

        grpcService.updateTenantSettings(request, observer);

        assertThat(result.get()).isNotNull();
        assertThat(result.get().getLoginMode()).isEqualTo("OIDC");
        assertThat(result.get().getDisplayName()).isEqualTo("My Blog");
        assertThat(result.get().getTagline()).isEqualTo("A tagline");
    }

    @Test
    @DisplayName("SWR-052: updateTenantSettings returns INVALID_ARGUMENT on bad tenant ID")
    void updateTenantSettings_invalidTenantId() {
        var request = UpdateTenantSettingsRequest.newBuilder()
                .setTenantId("not-a-uuid")
                .setLoginMode("BOTH")
                .setDisplayName("Blog")
                .build();

        var error = new AtomicReference<Throwable>();
        StreamObserver<TenantSettingsResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(TenantSettingsResponse value) {}

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }

            @Override
            public void onCompleted() {}
        };

        grpcService.updateTenantSettings(request, observer);

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode())
                .isEqualTo(Status.INVALID_ARGUMENT.getCode());
    }

    @Test
    @DisplayName("SWR-052: updateTenantSettings treats empty tagline as null")
    void updateTenantSettings_emptyTaglineAsNull() {
        var settings = TenantSettings.create(TenantId.of(TENANT_ID));
        when(tenantSettingsUseCase.updateSettings(any(), any(), eq(false), any(), any(), eq(null)))
                .thenReturn(settings);

        var request = UpdateTenantSettingsRequest.newBuilder()
                .setTenantId(TENANT_ID.toString())
                .setLoginMode("BOTH")
                .setDisplayName("Blog")
                .setTagline("")
                .build();

        var result = new AtomicReference<TenantSettingsResponse>();
        StreamObserver<TenantSettingsResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(TenantSettingsResponse value) {
                result.set(value);
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {}
        };

        grpcService.updateTenantSettings(request, observer);

        assertThat(result.get()).isNotNull();
    }

    @Test
    @DisplayName("SWR-053: listTenants returns all tenants via gRPC")
    void listTenants_returnsAll() {
        var s1 = TenantSettings.reconstitute(TenantId.of(TENANT_ID), LoginMode.BOTH, false, Set.of(), "Blog A", null);
        var id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        var s2 = TenantSettings.reconstitute(TenantId.of(id2), LoginMode.OIDC, false, Set.of(), "Blog B", "tagline");
        when(tenantSettingsUseCase.listAllTenants()).thenReturn(List.of(s1, s2));

        var request = ListTenantsRequest.newBuilder().build();

        var result = new AtomicReference<ListTenantsResponse>();
        StreamObserver<ListTenantsResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(ListTenantsResponse value) {
                result.set(value);
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {}
        };

        grpcService.listTenants(request, observer);

        assertThat(result.get().getTenantsList()).hasSize(2);
        assertThat(result.get().getTenants(0).getDisplayName()).isEqualTo("Blog A");
        assertThat(result.get().getTenants(1).getDisplayName()).isEqualTo("Blog B");
    }

    @Test
    @DisplayName("SWR-053: listTenants returns empty list when no tenants")
    void listTenants_empty() {
        when(tenantSettingsUseCase.listAllTenants()).thenReturn(List.of());

        var request = ListTenantsRequest.newBuilder().build();

        var result = new AtomicReference<ListTenantsResponse>();
        StreamObserver<ListTenantsResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(ListTenantsResponse value) {
                result.set(value);
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {}
        };

        grpcService.listTenants(request, observer);

        assertThat(result.get().getTenantsList()).isEmpty();
    }
}
