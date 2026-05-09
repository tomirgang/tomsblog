package de.tomsblog.usermanagement.adapter.inbound.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GrpcExceptionInterceptorTest {

    @Test
    @DisplayName("Interceptor delegates call successfully when no exception")
    void successfulCall_delegatesToListener() {
        var interceptor = new GrpcExceptionInterceptor();
        var delegateCalled = new AtomicBoolean(false);

        ServerCallHandler<Object, Object> handler = (c, headers) -> new ServerCall.Listener<>() {
            @Override
            public void onHalfClose() {
                delegateCalled.set(true);
            }
        };

        var listener = interceptor.interceptCall(createCall(new AtomicReference<>()), new Metadata(), handler);
        listener.onHalfClose();

        assertThat(delegateCalled.get()).isTrue();
    }

    @Test
    @DisplayName("Interceptor catches RuntimeException and returns INTERNAL")
    void catchesRuntimeException() {
        var interceptor = new GrpcExceptionInterceptor();
        var closedStatus = new AtomicReference<Status>();

        ServerCallHandler<Object, Object> handler = (c, headers) -> new ServerCall.Listener<>() {
            @Override
            public void onHalfClose() {
                throw new RuntimeException("unexpected error");
            }
        };

        var listener = interceptor.interceptCall(createCall(closedStatus), new Metadata(), handler);
        listener.onHalfClose();

        assertThat(closedStatus.get().getCode()).isEqualTo(Status.Code.INTERNAL);
    }

    @SuppressWarnings("unchecked")
    private ServerCall<Object, Object> createCall(AtomicReference<Status> closedStatus) {
        var methodDescriptor = MethodDescriptor.<Object, Object>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName("test/method")
                .setRequestMarshaller(new NoOpMarshaller())
                .setResponseMarshaller(new NoOpMarshaller())
                .build();

        return new ServerCall<>() {
            @Override
            public void request(int numMessages) {}

            @Override
            public void sendHeaders(Metadata headers) {}

            @Override
            public void sendMessage(Object message) {}

            @Override
            public void close(Status status, Metadata trailers) {
                closedStatus.set(status);
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public MethodDescriptor<Object, Object> getMethodDescriptor() {
                return methodDescriptor;
            }
        };
    }

    private static class NoOpMarshaller implements MethodDescriptor.Marshaller<Object> {
        @Override
        public InputStream stream(Object value) {
            return InputStream.nullInputStream();
        }

        @Override
        public Object parse(InputStream stream) {
            return null;
        }
    }
}
