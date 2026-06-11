package ${package}.shared.util;

import ${package}.shared.enums.ErrorCodeEnum;
import ${package}.shared.exception.AppException;
import io.github.archetom.common.result.Result;
import io.github.archetom.common.error.ErrorCode;
import io.github.archetom.common.error.ErrorContext;

/**
 * result process class
 */
public class ResultUtil {

    /**
     * generate error result.
     *
     * @param ex exception
     * @param appName application
     * @return error result
     */
    public static <T> Result<T> genErrorResult(Throwable ex, String appName) {

        final Result<T> result = new Result<>();

        result.setSuccess(false);

        ErrorCodeEnum errorCodeEnum = ErrorCodeEnum.UNKNOWN_EXP;

        ErrorContext errorContext = ErrorUtil.makeAndAddError(
                new ErrorCode(errorCodeEnum.getCompleteCode("9999"), ErrorCodeEnum.VERSION),
                String.format("%s:%s", errorCodeEnum.getDescription(), ex.getMessage()), appName);

        result.setErrorContext(errorContext);
        return result;
    }

    /**
     * based on exception generate error context add to result in
     *
     * @param result query result
     * @param e exception
     * @return encapsulate of result
     */
    public static <T> Result<T> genErrorResult(Result<T> result, AppException e,
                                               String eventCode, String appName) {

        result.setSuccess(false);
        ErrorContext errorContext = ErrorUtil.makeAndAddError(
                new ErrorCode(e.getErrorCode().getCompleteCode(eventCode), ErrorCodeEnum.VERSION),
                e.getMessage(), appName);
        result.setErrorContext(errorContext);
        return result;
    }
}
