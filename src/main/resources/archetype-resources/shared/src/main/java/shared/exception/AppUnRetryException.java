package ${package}.shared.exception;

import ${package}.shared.enums.ErrorCodeEnum;
import io.github.archetom.common.error.ErrorContext;

import java.io.Serial;

/**
 * 应用无需重试异常
 */
public class AppUnRetryException extends AppException {
    @Serial
    private static final long serialVersionUID = 6445665908006351745L;

    /**
     * 构造方法
     *
     * @param errorCode    异常CODE
     * @param errorContext 错误上下文
     */
    public AppUnRetryException(final ErrorCodeEnum errorCode, final ErrorContext errorContext) {
        super(errorCode, errorContext);
    }

    /**
     * 构造方法
     *
     * @param errorCode 异常
     * @param msg       异常信息
     */
    public AppUnRetryException(final ErrorCodeEnum errorCode, final String msg) {
        super(errorCode, msg);
    }

    /**
     * 构造方法
     *
     * @param errorCode 异常CODE
     */
    public AppUnRetryException(final ErrorCodeEnum errorCode) {
        super(errorCode);
    }
}
