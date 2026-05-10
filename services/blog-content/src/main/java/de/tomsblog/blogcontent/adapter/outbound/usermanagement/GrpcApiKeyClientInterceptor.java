package de.tomsblog.blogcontent.adapter.outbound.usermanagement;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * gRPC client interceptor that sends the API key as metadata for inter-service authentication.
 *
 * <p>Activated only when {@code service.grpc-api-key} is configured.
 */
@GrpcGlobalClientInterceptor
@ConditionalOnProperty("service.grpc-api-key")
public class GrpcApiKeyClientInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> API_KEY_HEADER =
            Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);

    private final String apiKey;

    public GrpcApiKeyClientInterceptor(@Value("${service.grpc-api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(API_KEY_HEADER, apiKey);
                super.start(responseListener, headers);
            }
        };
    }
}
