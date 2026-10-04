#set( $dollar = '$' )
package ${package}.infra.grpc.support;

import ${package}.shared.enums.ApplicationErrorCode;
import com.google.protobuf.Any;
import com.google.rpc.ErrorInfo;
import io.github.archetom.common.error.CommonError;
import io.github.archetom.common.result.Result;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Runs facade calls for gRPC services and turns their failures into gRPC statuses.
 *
 * <p>A failure keeps the safe public message as the status description and the stable public
 * error code as the {@code reason} of a {@code google.rpc.ErrorInfo} detail, mirroring the
 * HTTP error body.</p>
 */
@Component
public class GrpcFacadeCalls {

    /** Error scene shared with HTTP request validation and unclassified failures. */
    private static final String ADAPTER_SCENE = "9999";

    private final String appName;
    private final Validator validator;

    public GrpcFacadeCalls(@Value("${dollar}{spring.application.name}") String appName, Validator validator) {
        this.appName = appName;
        this.validator = validator;
    }

    /** Completes a unary call with the supplied response, or fails it with its gRPC status. */
    public <T> void unary(StreamObserver<T> observer, Supplier<T> call) {
        T response;
        try {
            response = call.get();
        } catch (StatusRuntimeException failure) {
            observer.onError(failure);
            return;
        }
        observer.onNext(response);
        observer.onCompleted();
    }

    /** Applies the API request's Bean Validation constraints, as the HTTP adapter does. */
    public <T> T validated(T request) {
        Optional<ConstraintViolation<T>> violation = validator.validate(request).stream()
                .min(Comparator.comparing(candidate -> candidate.getPropertyPath().toString()));
        if (violation.isEmpty()) {
            return request;
        }
        ApplicationErrorCode errorCode = ApplicationErrorCode.PARAMETER_INVALID;
        throw failure(errorCode, errorCode.getCompleteCode(ADAPTER_SCENE),
                violation.get().getPropertyPath() + ": " + violation.get().getMessage());
    }

    /** Returns the successful data, or throws the gRPC status for the facade's stable error. */
    public <T> T unwrap(Result<T> result) {
        if (result.isSuccess()) {
            return result.getData();
        }
        CommonError error = result.getErrorContext() == null ? null : result.getErrorContext().fetchRootError();
        if (error == null || error.getErrorCode() == null) {
            ApplicationErrorCode unknown = ApplicationErrorCode.UNKNOWN;
            throw failure(unknown, unknown.getCompleteCode(ADAPTER_SCENE), unknown.getDescription());
        }
        ApplicationErrorCode errorCode = ApplicationErrorCode.fromCode(error.getErrorCode().getErrorSpecific());
        throw failure(errorCode, error.getErrorCode().toString(), error.getErrorMsg());
    }

    static Status.Code statusCode(ApplicationErrorCode errorCode) {
        return switch (errorCode) {
            case PARAMETER_INVALID -> Status.Code.INVALID_ARGUMENT;
            case AUTHENTICATION_REQUIRED -> Status.Code.UNAUTHENTICATED;
            case ACCESS_DENIED -> Status.Code.PERMISSION_DENIED;
            case RESOURCE_NOT_FOUND -> Status.Code.NOT_FOUND;
            case RESOURCE_ALREADY_EXISTS -> Status.Code.ALREADY_EXISTS;
            case VERSION_CONFLICT, CONCURRENT_OPERATION -> Status.Code.ABORTED;
            case DOMAIN_RULE_VIOLATION, OPERATION_NOT_ALLOWED -> Status.Code.FAILED_PRECONDITION;
            case UNKNOWN, SYSTEM -> Status.Code.INTERNAL;
        };
    }

    private StatusRuntimeException failure(ApplicationErrorCode errorCode, String publicCode, String message) {
        com.google.rpc.Status status = com.google.rpc.Status.newBuilder()
                .setCode(statusCode(errorCode).value())
                .setMessage(message == null ? errorCode.getDescription() : message)
                .addDetails(Any.pack(ErrorInfo.newBuilder()
                        .setReason(publicCode)
                        .setDomain(appName)
                        .build()))
                .build();
        return StatusProto.toStatusRuntimeException(status);
    }
}
