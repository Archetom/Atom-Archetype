package ${package}.shared.exception;

import ${package}.shared.enums.ErrorCodeEnum;
import io.github.archetom.common.error.ErrorContext;

import java.io.Serial;

/**
 * application non-retryable exception
 */
public class AppUnRetryException extends AppException {
    @Serial
    private static final long serialVersionUID = 6445665908006351745L;

    /**
     * method
     *
     * @param errorCode exception CODE
     * @param errorContext error context
     */
    public AppUnRetryException(final ErrorCodeEnum errorCode, final ErrorContext errorContext) {
        super(errorCode, errorContext);
    }

    /**
     * method
     *
     * @param errorCode exception
     * @param msg exception
     */
    public AppUnRetryException(final ErrorCodeEnum errorCode, final String msg) {
        super(errorCode, msg);
    }

    /**
     * method
     *
     * @param errorCode exception CODE
     */
    public AppUnRetryException(final ErrorCodeEnum errorCode) {
        super(errorCode);
    }
}
