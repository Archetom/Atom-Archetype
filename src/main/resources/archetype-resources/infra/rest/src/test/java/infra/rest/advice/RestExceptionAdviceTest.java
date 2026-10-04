#set( $dollar = '$' )
package ${package}.infra.rest.advice;

import ${package}.domain.exception.DomainError;
import ${package}.domain.exception.DomainException;
import ${package}.domain.exception.InvalidValueException;
import ${package}.infra.rest.result.RestErrorResult;
import ${package}.shared.enums.ApplicationErrorCode;
import ${package}.shared.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class RestExceptionAdviceTest {

    private final RestExceptionAdvice advice = new RestExceptionAdvice("test-app");

    @Test
    void shouldMapParameterErrorToBadRequest() {
        ResponseEntity<?> response = advice.applicationException(
                new ApplicationException(ApplicationErrorCode.PARAMETER_INVALID, "ID must be positive"));

        assertError(response, HttpStatus.BAD_REQUEST, "101", "ID must be positive");
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
    void shouldMapInvalidValueToBadRequest() {
        ResponseEntity<?> response = advice.domainException(
                new InvalidValueException("Phone number must use E.164 format"));

        assertError(response, HttpStatus.BAD_REQUEST, "101", "Phone number must use E.164 format");
    }

    @Test
    void shouldTreatBareIllegalArgumentAsInternalFailure(CapturedOutput output) {
        String internalDetail = "tenant 7 does not match tenant 9";
        ResponseEntity<?> response = advice.unexpectedException(
                new IllegalArgumentException(internalDetail));

        RestErrorResult error = assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "000",
                ApplicationErrorCode.UNKNOWN.getDescription());
        assertFalse(error.getErrMsg().contains(internalDetail));
        assertTrue(output.getAll().contains(internalDetail));
    }

    @Test
    void shouldMapNotFoundToNotFound() {
        ResponseEntity<?> response = advice.domainException(
                new TestDomainException(DomainError.NOT_FOUND, "Resource does not exist"));

        assertError(response, HttpStatus.NOT_FOUND, "300", "Resource does not exist");
    }

    @Test
    void shouldMapDuplicateToConflictWithItsMessage() {
        ResponseEntity<?> response = advice.domainException(
                new TestDomainException(DomainError.ALREADY_EXISTS, "Email already exists"));

        assertError(response, HttpStatus.CONFLICT, "302", "Email already exists");
    }

    @Test
    void shouldMapVersionConflictToConflict() {
        ResponseEntity<?> response = advice.domainException(
                new TestDomainException(DomainError.VERSION_CONFLICT, "Resource version conflict"));

        assertError(response, HttpStatus.CONFLICT, "200", "Resource version conflict");
    }

    @Test
    void shouldMapGenericDomainRuleToUnprocessableContent() {
        ResponseEntity<?> response = advice.domainException(
                new TestDomainException(DomainError.RULE_VIOLATION, "Deleted records cannot change status"));

        assertError(response, HttpStatus.UNPROCESSABLE_CONTENT, "303",
                "Deleted records cannot change status");
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
    void shouldHideUnexpectedFailureFromClientButLogItInFull(CapturedOutput output) {
        String internalMessage = "lock wait timeout on t_user row 42";
        ResponseEntity<?> response = advice.unexpectedException(new RuntimeException(internalMessage));

        RestErrorResult error = assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "000",
                ApplicationErrorCode.UNKNOWN.getDescription());
        assertFalse(error.getErrMsg().contains(internalMessage));
        // Logs are internal: they keep the message and the stack trace for diagnosis.
        assertTrue(output.getAll().contains(internalMessage));
        assertTrue(output.getAll().contains("shouldHideUnexpectedFailureFromClientButLogItInFull"));
    }

    @Test
    void shouldKeepSpringMvcClientErrorStatusInsteadOfReportingServerFailure(CapturedOutput output) throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new PingController())
                .setControllerAdvice(advice)
                .build();

        mockMvc.perform(patch("/ping"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, org.hamcrest.Matchers.containsString("GET")))
                .andExpect(jsonPath("${dollar}.errMsg").value("Method Not Allowed"));
        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("${dollar}.errCode").value(org.hamcrest.Matchers.endsWith("300")));

        assertFalse(output.getAll().contains("Unexpected request failure"));
    }

    /** Generic domain failure, so these tests do not depend on the removable User sample. */
    private static final class TestDomainException extends DomainException {

        private TestDomainException(DomainError error, String message) {
            super(error, message);
        }
    }

    @RestController
    static class PingController {

        @GetMapping("/ping")
        String ping() {
            return "pong";
        }
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
