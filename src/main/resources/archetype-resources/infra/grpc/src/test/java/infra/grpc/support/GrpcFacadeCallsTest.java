package ${package}.infra.grpc.support;

import ${package}.api.dto.request.QueryRequest;
import ${package}.shared.enums.ApplicationErrorCode;
import ${package}.shared.exception.NonRetryableApplicationException;
import ${package}.shared.util.ResultUtil;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.ErrorInfo;
import io.github.archetom.common.result.Result;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrpcFacadeCallsTest {

    private final GrpcFacadeCalls calls = new GrpcFacadeCalls(
            "grpc-test", Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void completesTheCallWithTheResponse() {
        RecordingObserver<String> observer = new RecordingObserver<>();

        calls.unary(observer, () -> calls.unwrap(success("done")));

        assertEquals(List.of("done"), observer.values);
        assertTrue(observer.completed);
    }

    @Test
    void mapsStableErrorsToGrpcStatusesAndKeepsThePublicCode() throws InvalidProtocolBufferException {
        Map<ApplicationErrorCode, Status.Code> expected = Map.of(
                ApplicationErrorCode.PARAMETER_INVALID, Status.Code.INVALID_ARGUMENT,
                ApplicationErrorCode.AUTHENTICATION_REQUIRED, Status.Code.UNAUTHENTICATED,
                ApplicationErrorCode.ACCESS_DENIED, Status.Code.PERMISSION_DENIED,
                ApplicationErrorCode.RESOURCE_NOT_FOUND, Status.Code.NOT_FOUND,
                ApplicationErrorCode.RESOURCE_ALREADY_EXISTS, Status.Code.ALREADY_EXISTS,
                ApplicationErrorCode.VERSION_CONFLICT, Status.Code.ABORTED,
                ApplicationErrorCode.DOMAIN_RULE_VIOLATION, Status.Code.FAILED_PRECONDITION,
                ApplicationErrorCode.UNKNOWN, Status.Code.INTERNAL);

        for (Map.Entry<ApplicationErrorCode, Status.Code> entry : expected.entrySet()) {
            StatusRuntimeException failure = assertThrows(StatusRuntimeException.class,
                    () -> calls.unwrap(failure(entry.getKey(), "Safe message")));

            assertEquals(entry.getValue(), failure.getStatus().getCode(), entry.getKey().name());
            ErrorInfo errorInfo = errorInfo(failure);
            assertEquals(entry.getKey().getCompleteCode("1000"), errorInfo.getReason());
            assertEquals("grpc-test", errorInfo.getDomain());
        }
    }

    @Test
    void failsTheCallWithItsStatusInsteadOfThrowing() {
        RecordingObserver<String> observer = new RecordingObserver<>();

        calls.unary(observer, () -> calls.unwrap(failure(ApplicationErrorCode.RESOURCE_NOT_FOUND, "Missing")));

        assertTrue(observer.values.isEmpty());
        assertEquals(Status.Code.NOT_FOUND, Status.fromThrowable(observer.error).getCode());
        assertEquals("Missing", Status.fromThrowable(observer.error).getDescription());
    }

    @Test
    void rejectsInvalidRequestsWithTheHttpValidationCode() throws InvalidProtocolBufferException {
        StatusRuntimeException failure = assertThrows(StatusRuntimeException.class,
                () -> calls.validated(new QueryRequest().setSize(QueryRequest.MAX_SIZE + 1)));

        assertEquals(Status.Code.INVALID_ARGUMENT, failure.getStatus().getCode());
        assertTrue(failure.getStatus().getDescription().startsWith("size: "));
        assertEquals(ApplicationErrorCode.PARAMETER_INVALID.getCompleteCode("9999"), errorInfo(failure).getReason());
    }

    private static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    private static <T> Result<T> failure(ApplicationErrorCode errorCode, String message) {
        return ResultUtil.genErrorResult(new Result<>(),
                new NonRetryableApplicationException(errorCode, message), "1000", "grpc-test");
    }

    private static ErrorInfo errorInfo(StatusRuntimeException failure) throws InvalidProtocolBufferException {
        return StatusProto.fromThrowable(failure).getDetails(0).unpack(ErrorInfo.class);
    }

    private static final class RecordingObserver<T> implements StreamObserver<T> {

        private final List<T> values = new ArrayList<>();
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(T value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable throwable) {
            error = throwable;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }
}
