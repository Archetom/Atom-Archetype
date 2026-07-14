package ${package}.infra.rest.advice;

import ${package}.domain.exception.UserAlreadyExistsException;
import ${package}.domain.exception.UserDomainException;
import ${package}.domain.exception.UserNotFoundException;
import ${package}.infra.rest.result.RestErrorResult;
import ${package}.shared.enums.ApplicationErrorCode;
import ${package}.shared.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class RestExceptionAdviceTest {

    private final RestExceptionAdvice advice = new RestExceptionAdvice("test-app");

    @Test
    void shouldMapParameterErrorToBadRequest() {
        ResponseEntity<?> response = advice.applicationException(
                new ApplicationException(ApplicationErrorCode.PARAMETER_INVALID, "User ID must be positive"));

        assertError(response, HttpStatus.BAD_REQUEST, "101", "User ID must be positive");
    }

    @Test
    void shouldMapAuthenticationAndAuthorizationErrors() {
        ResponseEntity<?> unauthenticated = advice.applicationException(
                new ApplicationException(ApplicationErrorCode.AUTHENTICATION_REQUIRED));
        ResponseEntity<?> forbidden = advice.applicationException(
                new ApplicationException(ApplicationErrorCode.ACCESS_DENIED));

        assertError(unauthenticated, HttpStatus.UNAUTHORIZED, "102", "Authentication is required");
        assertError(forbidden, HttpStatus.FORBIDDEN, "103", "Access is denied");
    }

    @Test
    void shouldMapBoundaryAccessDeniedToForbidden() {
        ResponseEntity<?> response = advice.accessDeniedException(
                new AccessDeniedException("internal authorization details"));

        assertError(response, HttpStatus.FORBIDDEN, "103", "Access is denied");
    }

    @Test
    void shouldMapIllegalArgumentToBadRequestWithoutLeakingValue(CapturedOutput output) {
        String rejectedValue = "invalid phone +secret";
        ResponseEntity<?> response = advice.illegalArgumentException(
                new IllegalArgumentException(rejectedValue));

        RestErrorResult error = assertError(response, HttpStatus.BAD_REQUEST, "101",
                "Request parameters are invalid");
        assertFalse(error.getErrMsg().contains(rejectedValue));
        assertFalse(output.getAll().contains(rejectedValue));
    }

    @Test
    void shouldMapNotFoundToNotFound() {
        ResponseEntity<?> response = advice.domainException(new UserNotFoundException(42L));

        assertError(response, HttpStatus.NOT_FOUND, "300", "User does not exist");
    }

    @Test
    void shouldMapEmailDuplicateToConflictWithCorrectMessage() {
        ResponseEntity<?> response = advice.domainException(
                UserAlreadyExistsException.byEmail("person@example.com"));

        assertError(response, HttpStatus.CONFLICT, "302", "Email already exists");
    }

    @Test
    void shouldMapGenericDomainRuleToUnprocessableContent() {
        ResponseEntity<?> response = advice.domainException(
                new UserDomainException("Deleted users cannot change status"));

        assertError(response, HttpStatus.UNPROCESSABLE_CONTENT, "303",
                "Deleted users cannot change status");
    }

    @Test
    void shouldRejectMalformedBodyWithoutLeakingParserMessage(CapturedOutput output) {
        String internalMessage = "Unexpected token near credential=secret";
        ResponseEntity<?> response = advice.parameterTypeException(
                new HttpMessageConversionException("conversion failed", new IllegalArgumentException(internalMessage)));

        RestErrorResult error = assertError(response, HttpStatus.BAD_REQUEST, "101",
                "Request body is malformed");
        assertFalse(error.getErrMsg().contains(internalMessage));
        assertFalse(output.getAll().contains(internalMessage));
    }

    @Test
    void shouldMapUnknownFailureToInternalServerErrorWithoutLeakingMessage(CapturedOutput output) {
        String internalMessage = "database password=secret";
        ResponseEntity<?> response = advice.unexpectedException(new RuntimeException(internalMessage));

        RestErrorResult error = assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "000",
                ApplicationErrorCode.UNKNOWN.getDescription());
        assertFalse(error.getErrMsg().contains(internalMessage));
        assertFalse(output.getAll().contains(internalMessage));
    }

    private RestErrorResult assertError(
            ResponseEntity<?> response,
            HttpStatus expectedStatus,
            String expectedSpecificCode,
            String expectedMessage
    ) {
        assertEquals(expectedStatus, response.getStatusCode());
        RestErrorResult error = assertInstanceOf(RestErrorResult.class, response.getBody());
        assertTrue(error.getErrCode().endsWith(expectedSpecificCode));
        assertEquals(expectedMessage, error.getErrMsg());
        return error;
    }
}
