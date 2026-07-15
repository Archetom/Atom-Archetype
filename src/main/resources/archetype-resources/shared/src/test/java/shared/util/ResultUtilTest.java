package ${package}.shared.util;

import ${package}.shared.enums.ApplicationErrorCode;
import ${package}.shared.exception.ApplicationException;
import io.github.archetom.common.error.CommonError;
import io.github.archetom.common.error.ErrorCode;
import io.github.archetom.common.error.ErrorContext;
import io.github.archetom.common.result.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResultUtilTest {

    private static final String EVENT_CODE = "1234";
    private static final String APP_NAME = "shared-test";

    @Test
    void hidesMessageForInternalErrors() {
        assertEquals(ApplicationErrorCode.SYSTEM.getDescription(),
                ResultUtil.publicMessage(ApplicationErrorCode.SYSTEM, "database password leaked"));
    }

    @Test
    void createsGenericPublicErrorForUnexpectedException() {
        Result<String> result = ResultUtil.genErrorResult(new IllegalStateException("database unavailable"), APP_NAME);

        CommonError rootError = result.getErrorContext().fetchRootError();
        assertFalse(result.isSuccess());
        assertEquals(ApplicationErrorCode.UNKNOWN.getCompleteCode("9999"), rootError.getErrorCode().toString());
        assertEquals(ApplicationErrorCode.UNKNOWN.getDescription(), rootError.getErrorMsg());
        assertEquals(APP_NAME, rootError.getLocation());
    }

    @Test
    void createsApplicationErrorWithPublicMessage() {
        Result<String> result = ResultUtil.genErrorResult(new Result<>(),
                new ApplicationException(ApplicationErrorCode.RESOURCE_NOT_FOUND, "User does not exist"),
                EVENT_CODE, APP_NAME);

        CommonError rootError = result.getErrorContext().fetchRootError();
        assertFalse(result.isSuccess());
        assertEquals(ApplicationErrorCode.RESOURCE_NOT_FOUND.getCompleteCode(EVENT_CODE),
                rootError.getErrorCode().toString());
        assertEquals("User does not exist", rootError.getErrorMsg());
        assertEquals(APP_NAME, rootError.getLocation());
    }

    @Test
    void appendsApplicationErrorToExceptionContextWhileRestKeepsOriginalRootError() {
        ErrorContext originalContext = ErrorUtil.makeAndAddError(
                new ErrorCode(ApplicationErrorCode.PARAMETER_INVALID.getCompleteCode(EVENT_CODE),
                        ApplicationErrorCode.VERSION),
                "Original validation error", "upstream");
        ApplicationException exception = new ApplicationException(ApplicationErrorCode.SYSTEM, originalContext);

        Result<String> result = ResultUtil.genErrorResult(new Result<>(), exception, EVENT_CODE, APP_NAME);

        CommonError rootError = result.getErrorContext().fetchRootError();
        CommonError currentError = result.getErrorContext().fetchCurrentError();
        assertSame(originalContext, result.getErrorContext());
        assertEquals(2, result.getErrorContext().getErrorStack().size());
        assertEquals(ApplicationErrorCode.PARAMETER_INVALID.getCompleteCode(EVENT_CODE),
                rootError.getErrorCode().toString());
        assertEquals("Original validation error", rootError.getErrorMsg());
        assertEquals(ApplicationErrorCode.SYSTEM.getCompleteCode(EVENT_CODE), currentError.getErrorCode().toString());
        assertEquals(ApplicationErrorCode.SYSTEM.getDescription(), currentError.getErrorMsg());
        assertEquals(APP_NAME, currentError.getLocation());
    }

    @Test
    void rejectsEventCodesThatCannotProduceACompleteErrorCode() {
        ApplicationException exception = new ApplicationException(ApplicationErrorCode.PARAMETER_INVALID, "Invalid input");

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> ResultUtil.genErrorResult(new Result<>(), exception, "123", APP_NAME));

        assertEquals("eventCode must be exactly 4 characters long", failure.getMessage());
    }
}
