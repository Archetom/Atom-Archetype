package ${package}.shared.util;

import ${package}.shared.enums.ErrorCodeEnum;
import ${package}.shared.exception.AppException;
import io.github.archetom.common.result.Result;
import io.github.archetom.common.error.ErrorCode;
import io.github.archetom.common.error.ErrorContext;

/**
 * 结果处理类
 */
public class ResultUtil {

    /**
     * 生成错误结果。
     *
     * @param ex      异常
     * @param appName 应用名称
     * @return 错误结果
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
     * 根据异常信息生成错误上下文添加到结果中
     *
     * @param result 查询结果
     * @param e      异常
     * @return 封装后的结果
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
