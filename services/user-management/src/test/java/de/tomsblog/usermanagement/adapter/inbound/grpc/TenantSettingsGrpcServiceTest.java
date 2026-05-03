package de.tomsblog.usermanagement.adapter.inbound.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.tomsblog.grpc.usermanagement.GetTenantSettingsRequest;
import de.tomsblog.grpc.usermanagement.TenantSettingsResponse;
import de.tomsblog.shared.tenant.TenantId;
import de.tomsblog.usermanagement.application.port.inbound.TenantSettingsUseCase;
import de.tomsblog.usermanagement.domain.model.TenantSettings;
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
}
