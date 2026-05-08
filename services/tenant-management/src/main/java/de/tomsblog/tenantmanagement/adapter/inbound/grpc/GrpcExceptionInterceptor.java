package de.tomsblog.tenantmanagement.adapter.inbound.grpc;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC interceptor that catches unchecked exceptions and returns proper gRPC status codes.
 *
 * @req SWR-074
 */
@GrpcGlobalServerInterceptor
public class GrpcExceptionInterceptor implements ServerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcExceptionInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {
            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (IllegalArgumentException e) {
                    call.close(Status.INVALID_ARGUMENT.withDescription(e.getMessage()), new Metadata());
                } catch (RuntimeException e) {
                    LOG.error(
                            "Unexpected error in gRPC call {}",
                            call.getMethodDescriptor().getFullMethodName(),
                            e);
                    call.close(Status.INTERNAL.withDescription("Internal server error"), new Metadata());
                }
            }
        };
    }
}
