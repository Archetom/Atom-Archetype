package ${package}.shared.util;

import io.github.archetom.common.error.CommonError;
import io.github.archetom.common.error.ErrorCode;
import io.github.archetom.common.error.ErrorContext;

/** Builds the error primitives used by the public result contract. */
public final class ErrorUtil {

    private ErrorUtil() {
    }

    /** Creates a context holding one located error. */
    public static ErrorContext makeAndAddError(ErrorCode errorCode, String message, String location) {
        return makeAndAddError(null, errorCode, message, location);
    }

    /** Adds a located error to an existing context, or to a new context when none is supplied. */
    public static ErrorContext makeAndAddError(ErrorContext context, ErrorCode errorCode, String message,
                                               String location) {
        CommonError error = new CommonError();
        error.setLocation(location);
        error.setErrorCode(errorCode);
        error.setErrorMsg(message);
        ErrorContext target = context != null ? context : new ErrorContext();
        target.addError(error);
        return target;
    }
}
