package ${package}.shared.exception;


import ${package}.shared.enums.ErrorCodeEnum;
import io.github.archetom.common.error.ErrorContext;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 应用需要重试的异常
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 4718379562894293206L;

    /**
     * 异常错误代码
     */
    private ErrorCodeEnum errorCode;

    /**
     * 错误上下文
     */
    private ErrorContext errorContext;

    /**
     * 错误上下文
     */
    private Throwable originalThrowable;

    /**
     * 创建一DataQueryException
     *
     * @param errorCode    系统定义异常 当前异常
     * @param errorContext 外部系统发生异常
     */
    @SuppressWarnings("unused")
    public AppException(ErrorCodeEnum errorCode, ErrorContext errorContext) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
        this.errorContext = errorContext;
    }

    /**
     * 创建一DataQueryException
     *
     * <p>内部异常时使用</p>
     *
     * @param errorCode 内部错误码
     * @param message   异常信息
     */
    public AppException(ErrorCodeEnum errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 创建一DataQueryException
     *
     * @param errorCode 错误码
     */
    public AppException(ErrorCodeEnum errorCode, Throwable originalThrowable) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
        this.originalThrowable = originalThrowable;
    }

    /**
     * 创建一DataQueryException,保存原始异常信息
     *
     * @param errorCode 错误码
     */
    public AppException(ErrorCodeEnum errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
    }

    /**
     * 异常构造函数
     *
     * @param errorCode 错误码
     * @param message   异常信息
     * @param e         异常堆栈
     */
    public AppException(ErrorCodeEnum errorCode, String message, Throwable e) {
        super(message, e);
        this.errorCode = errorCode;
    }
}
