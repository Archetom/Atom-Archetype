package ${package}.shared.util;

import io.github.archetom.common.error.CommonError;
import io.github.archetom.common.error.ErrorCode;
import io.github.archetom.common.error.ErrorContext;

import lombok.extern.slf4j.Slf4j;

/**
 * error utility class
 */
@Slf4j
public class ErrorUtil {

    /**
     * build error code
     */
    public static ErrorCode makeErrorCode(String errorLevel, String errorType, String errorScene, String errorSpecific) {
        return new ErrorCode(errorLevel, errorType, errorScene, errorSpecific);
    }

    /**
     * error
     *
     * @param errorCode error code
     * @param message error message
     * @return error
     */
    private static CommonError makeError(ErrorCode errorCode, String message) {
        CommonError error = new CommonError();
        error.setErrorCode(errorCode);
        error.setErrorMsg(message);
        return error;
    }

    /**
     * error
     *
     * @param errorCode error code
     * @param message error message
     * @param location error position
     * @return error
     */
    private static CommonError makeError(ErrorCode errorCode, String message, String location) {
        CommonError error = new CommonError();
        error.setLocation(location);
        error.setErrorCode(errorCode);
        error.setErrorMsg(message);
        return error;
    }

    /**
     * new error to error context
     */
    private static ErrorContext addError(CommonError error) {
        return addError(null, error);
    }

    /**
     * new error
     *
     * @param context error context
     * @param error error
     * @return of error context
     */
    private static ErrorContext addError(ErrorContext context, CommonError error) {
        if (context == null) {
            context = new ErrorContext();
        }
        if (error == null) {
            log.error(" parameter invalid, of error class is empty ");
            return context;
        }

        context.addError(error);

        return context;
    }

    /**
     * error error context
     *
     * @param context error context
     * @param errorCode error code
     * @param message error message
     * @return of error context
     */
    public static ErrorContext makeAndAddError(ErrorContext context, ErrorCode errorCode, String message) {
        CommonError error = makeError(errorCode, message);
        context = addError(context, error);

        return context;
    }

    /**
     * error error context
     *
     * @param errorCode error code
     * @param message error message
     * @param location exception of application
     * @return of error context
     */
    public static ErrorContext makeAndAddError(ErrorCode errorCode, String message, String location) {
        CommonError error = makeError(errorCode, message, location);
        return addError(error);
    }
}
