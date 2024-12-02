package ${package}.shared.util;

import io.github.archetom.common.error.CommonError;
import io.github.archetom.common.error.ErrorCode;
import io.github.archetom.common.error.ErrorContext;

import lombok.extern.slf4j.Slf4j;

/**
 * 错误工具类
 */
@Slf4j
public class ErrorUtil {

    /**
     * 构建错误码
     */
    public static ErrorCode makeErrorCode(String errorLevel, String errorType, String errorScene, String errorSpecific) {
        return new ErrorCode(errorLevel, errorType, errorScene, errorSpecific);
    }

    /**
     * 构造错误
     *
     * @param errorCode 错误码
     * @param message   错误信息
     * @return 错误
     */
    private static CommonError makeError(ErrorCode errorCode, String message) {
        CommonError error = new CommonError();
        error.setErrorCode(errorCode);
        error.setErrorMsg(message);
        return error;
    }

    /**
     * 构造错误
     *
     * @param errorCode 错误码
     * @param message   错误信息
     * @param location  错误位置
     * @return 错误
     */
    private static CommonError makeError(ErrorCode errorCode, String message, String location) {
        CommonError error = new CommonError();
        error.setLocation(location);
        error.setErrorCode(errorCode);
        error.setErrorMsg(message);
        return error;
    }

    /**
     * 新增错误 到错误上下文
     */
    private static ErrorContext addError(CommonError error) {
        return addError(null, error);
    }

    /**
     * 新增错误
     *
     * @param context 错误上下文
     * @param error   错误
     * @return 变化后的错误上下文
     */
    private static ErrorContext addError(ErrorContext context, CommonError error) {
        if (context == null) {
            context = new ErrorContext();
        }
        if (error == null) {
            log.error("参数非法，传入的错误类为空");
            return context;
        }

        context.addError(error);

        return context;
    }

    /**
     * 构造错误并 写入错误上下文
     *
     * @param context   错误上下文
     * @param errorCode 错误编码
     * @param message   错误信息
     * @return 变化后的错误上下文
     */
    public static ErrorContext makeAndAddError(ErrorContext context, ErrorCode errorCode, String message) {
        CommonError error = makeError(errorCode, message);
        context = addError(context, error);

        return context;
    }

    /**
     * 构造错误并 写入错误上下文
     *
     * @param errorCode 错误编码
     * @param message   错误信息
     * @param location  异常发生的应用
     * @return 变化后的错误上下文
     */
    public static ErrorContext makeAndAddError(ErrorCode errorCode, String message, String location) {
        CommonError error = makeError(errorCode, message, location);
        return addError(error);
    }
}
