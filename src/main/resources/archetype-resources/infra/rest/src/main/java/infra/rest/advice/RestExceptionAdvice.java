#set( $dollar = '$' )
package ${package}.infra.rest.advice;

import ${package}.application.exception.DomainExceptionMapper;
import ${package}.domain.exception.DomainException;
import ${package}.infra.rest.util.ErrorResultWrapUtil;
import ${package}.infra.rest.util.ResponseEntityUtil;
import ${package}.shared.enums.ApplicationErrorCode;
import ${package}.shared.exception.ApplicationException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Maps transport, application, and domain exceptions to stable HTTP error responses.
 */
@Slf4j
@RestControllerAdvice
public class RestExceptionAdvice {

    private final String appName;

    public RestExceptionAdvice(@Value("${dollar}{spring.application.name}") String appName) {
        this.appName = appName;
    }

    /** Maps request-body validation failures to a safe parameter error. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validationBodyException(MethodArgumentNotValidException exception) {
        return ResponseEntityUtil.assembleResponse(ErrorResultWrapUtil.genErrorResultValidation(exception, appName));
    }

    /** Maps malformed request bodies without logging their potentially sensitive contents. */
    @ExceptionHandler(HttpMessageConversionException.class)
    public ResponseEntity<?> parameterTypeException(HttpMessageConversionException exception) {
        log.warn("Request body conversion failed: exceptionType={}", exception.getClass().getName());
        return ResponseEntityUtil.assembleResponse(ErrorResultWrapUtil.genErrorResultValidation(exception, appName));
    }

    /** Maps request-parameter binding and constraint failures. */
    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<?> requestParameterException(Exception exception) {
        log.warn("Request parameter validation failed: {}", exception.getClass().getSimpleName());
        return ResponseEntityUtil.assembleResponse(ErrorResultWrapUtil.genErrorResult(
                ApplicationErrorCode.PARAMETER_INVALID, "Request parameters are invalid", appName));
    }

    /** Maps explicit boundary authorization rejection to HTTP 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> accessDeniedException(AccessDeniedException exception) {
        log.warn("Request access denied: exceptionType={}", exception.getClass().getName());
        return ResponseEntityUtil.assembleResponse(ErrorResultWrapUtil.genErrorResult(
                ApplicationErrorCode.ACCESS_DENIED,
                ApplicationErrorCode.ACCESS_DENIED.getDescription(), appName));
    }

    /**
     * Preserves explicit codes for invalid values, domain rules, missing resources, and duplicates.
     * A bare {@link IllegalArgumentException} is a programming error and falls through to HTTP 500.
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<?> domainException(DomainException exception) {
        ApplicationErrorCode errorCode = DomainExceptionMapper.toApplicationCode(exception);
        log.warn("Domain request rejected: error={}", exception.getError());
        return ResponseEntityUtil.assembleResponse(ErrorResultWrapUtil.genErrorResult(
                errorCode, exception.getMessage(), appName));
    }

    /** Maps an expected application-layer rejection. */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<?> applicationException(ApplicationException exception) {
        log.warn("Application request rejected: code={}", exception.getErrorCode());
        return ResponseEntityUtil.assembleResponse(ErrorResultWrapUtil.genErrorResult(
                exception.getErrorCode(), exception.getMessage(), appName));
    }

    /** Preserves an already-classified, operation-scoped failure at the REST boundary. */
    @ExceptionHandler(ResultResponseException.class)
    public ResponseEntity<?> resultResponseException(ResultResponseException exception) {
        log.warn("Application result rejected at REST boundary");
        return ResponseEntityUtil.assembleResponse(exception.result());
    }

    /**
     * Maps an unexpected failure without exposing its message or cause. Spring MVC's own client
     * errors, such as an unknown route (404) or unsupported method (405), keep their status.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpectedException(Exception exception) {
        if (exception instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            return frameworkClientError(exception, errorResponse.getStatusCode(), errorResponse.getHeaders());
        }
        log.error("Unexpected request failure", exception);
        return ResponseEntityUtil.assembleResponse(ErrorResultWrapUtil.genErrorResult(
                ApplicationErrorCode.UNKNOWN, null, appName));
    }

    private ResponseEntity<?> frameworkClientError(Exception exception, HttpStatusCode status, HttpHeaders headers) {
        log.warn("Request rejected by Spring MVC: status={}, exceptionType={}",
                status.value(), exception.getClass().getName());
        ApplicationErrorCode errorCode = status.value() == HttpStatus.NOT_FOUND.value()
                ? ApplicationErrorCode.RESOURCE_NOT_FOUND
                : ApplicationErrorCode.PARAMETER_INVALID;
        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        // The standard reason phrase never echoes request data such as the path or media type.
        String message = knownStatus != null ? knownStatus.getReasonPhrase() : errorCode.getDescription();
        return ResponseEntityUtil.assembleFailure(
                ErrorResultWrapUtil.genErrorResult(errorCode, message, appName), status, headers);
    }
}
