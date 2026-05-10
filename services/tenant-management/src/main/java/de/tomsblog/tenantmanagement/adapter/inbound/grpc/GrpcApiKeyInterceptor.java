package de.tomsblog.tenantmanagement.adapter.inbound.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * gRPC server interceptor that enforces API key authentication for inter-service calls.
 *
 * <p>Activated only when {@code service.grpc-api-key} is configured. Validates the
 * {@code x-api-key} metadata header against the expected key using timing-safe comparison.
 */
@GrpcGlobalServerInterceptor
@ConditionalOnProperty("service.grpc-api-key")
public class GrpcApiKeyInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> API_KEY_HEADER =
            Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);

    private final String expectedApiKey;

    public GrpcApiKeyInterceptor(@Value("${service.grpc-api-key}") String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String providedKey = headers.get(API_KEY_HEADER);
        if (providedKey != null
                && MessageDigest.isEqual(
                        expectedApiKey.getBytes(StandardCharsets.UTF_8),
                        providedKey.getBytes(StandardCharsets.UTF_8))) {
            return next.startCall(call, headers);
        }
        call.close(Status.UNAUTHENTICATED.withDescription("Invalid or missing API key"), new Metadata());
        return new ServerCall.Listener<>() {};
    }
}
