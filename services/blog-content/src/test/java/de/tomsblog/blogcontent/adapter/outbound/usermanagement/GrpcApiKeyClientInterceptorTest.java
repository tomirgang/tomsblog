package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GrpcApiKeyClientInterceptorTest {

    private static final String API_KEY = "test-api-key-12345";
    private static final Metadata.Key<String> API_KEY_HEADER =
            Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);

    private final GrpcApiKeyClientInterceptor interceptor = new GrpcApiKeyClientInterceptor(API_KEY);

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Adds x-api-key header to outgoing gRPC call")
    void addsApiKeyHeader() {
        MethodDescriptor<Object, Object> method = mock(MethodDescriptor.class);
        CallOptions callOptions = CallOptions.DEFAULT;
        Channel channel = mock(Channel.class);
        ClientCall<Object, Object> delegateCall = mock(ClientCall.class);
        when(channel.newCall(any(), any())).thenReturn(delegateCall);

        ClientCall<Object, Object> wrappedCall = interceptor.interceptCall(method, callOptions, channel);

        Metadata headers = new Metadata();
        wrappedCall.start(mock(ClientCall.Listener.class), headers);

        assertThat(headers.get(API_KEY_HEADER)).isEqualTo(API_KEY);
        verify(delegateCall).start(any(), eq(headers));
    }
}
