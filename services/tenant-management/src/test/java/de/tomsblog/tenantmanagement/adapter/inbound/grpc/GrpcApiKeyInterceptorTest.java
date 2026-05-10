package de.tomsblog.tenantmanagement.adapter.inbound.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GrpcApiKeyInterceptorTest {

    private static final String EXPECTED_KEY = "test-api-key-12345";
    private static final Metadata.Key<String> API_KEY_HEADER =
            Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);

    private final GrpcApiKeyInterceptor interceptor = new GrpcApiKeyInterceptor(EXPECTED_KEY);

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Allows call with valid API key")
    void allowsCallWithValidApiKey() {
        ServerCall<Object, Object> call = mock(ServerCall.class);
        ServerCallHandler<Object, Object> next = mock(ServerCallHandler.class);
        ServerCall.Listener<Object> expectedListener = new ServerCall.Listener<>() {};
        when(next.startCall(any(), any())).thenReturn(expectedListener);

        Metadata headers = new Metadata();
        headers.put(API_KEY_HEADER, EXPECTED_KEY);

        ServerCall.Listener<Object> result = interceptor.interceptCall(call, headers, next);

        verify(next).startCall(call, headers);
        assertThat(result).isSameAs(expectedListener);
        verify(call, never()).close(any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Rejects call with wrong API key")
    void rejectsCallWithWrongApiKey() {
        ServerCall<Object, Object> call = mock(ServerCall.class);
        ServerCallHandler<Object, Object> next = mock(ServerCallHandler.class);

        Metadata headers = new Metadata();
        headers.put(API_KEY_HEADER, "wrong-key");

        interceptor.interceptCall(call, headers, next);

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(call).close(statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
        verify(next, never()).startCall(any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Rejects call with missing API key")
    void rejectsCallWithMissingApiKey() {
        ServerCall<Object, Object> call = mock(ServerCall.class);
        ServerCallHandler<Object, Object> next = mock(ServerCallHandler.class);

        Metadata headers = new Metadata();

        interceptor.interceptCall(call, headers, next);

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(call).close(statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
        verify(next, never()).startCall(any(), any());
    }
}
