package ${package}.shared.exception;


import ${package}.shared.enums.ErrorCodeEnum;
import io.github.archetom.common.error.ErrorContext;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * application need of exception
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 4718379562894293206L;

    /**
     * exception error code
     */
    private ErrorCodeEnum errorCode;

    /**
     * error context
     */
    private ErrorContext errorContext;

    /**
     * error context
     */
    private Throwable originalThrowable;

    /**
     * create DataQueryException
     *
     * @param errorCode system define exception current exception
     * @param errorContext external system exception
     */
    @SuppressWarnings("unused")
    public AppException(ErrorCodeEnum errorCode, ErrorContext errorContext) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
        this.errorContext = errorContext;
    }

    /**
     * create DataQueryException
     *
     * <p> internal exception </p>
     *
     * @param errorCode internal error code
     * @param message exception
     */
    public AppException(ErrorCodeEnum errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * create DataQueryException
     *
     * @param errorCode error code
     */
    public AppException(ErrorCodeEnum errorCode, Throwable originalThrowable) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
        this.originalThrowable = originalThrowable;
    }

    /**
     * create DataQueryException, save exception
     *
     * @param errorCode error code
     */
    public AppException(ErrorCodeEnum errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
    }

    /**
     * exception function
     *
     * @param errorCode error code
     * @param message exception
     * @param e exception
     */
    public AppException(ErrorCodeEnum errorCode, String message, Throwable e) {
        super(message, e);
        this.errorCode = errorCode;
    }
}
