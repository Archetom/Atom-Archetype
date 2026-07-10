package ${package}.shared.enums;

import ${package}.shared.consts.ErrorLevelConst;
import ${package}.shared.consts.ErrorTypeConst;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Stable application errors used to build public error codes.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ApplicationErrorCode {

    UNKNOWN("000", "Unexpected internal error", ErrorLevelConst.ERROR, ErrorTypeConst.SYSTEM),
    SYSTEM("001", "System error", ErrorLevelConst.ERROR, ErrorTypeConst.SYSTEM),
    OPERATION_NOT_ALLOWED("002", "Operation is not allowed", ErrorLevelConst.WARN, ErrorTypeConst.BIZ),
    PARAMETER_INVALID("101", "Request parameters are invalid", ErrorLevelConst.WARN, ErrorTypeConst.BIZ),
    AUTHENTICATION_REQUIRED("102", "Authentication is required", ErrorLevelConst.WARN, ErrorTypeConst.BIZ),
    ACCESS_DENIED("103", "Access is denied", ErrorLevelConst.WARN, ErrorTypeConst.BIZ),
    VERSION_CONFLICT("200", "Resource version conflict", ErrorLevelConst.WARN, ErrorTypeConst.BIZ),
    RESOURCE_NOT_FOUND("300", "Requested resource was not found", ErrorLevelConst.WARN, ErrorTypeConst.BIZ),
    RESOURCE_ALREADY_EXISTS("302", "Resource already exists", ErrorLevelConst.WARN, ErrorTypeConst.BIZ),
    DOMAIN_RULE_VIOLATION("303", "Domain rule violation", ErrorLevelConst.WARN, ErrorTypeConst.BIZ);

    public static final String VERSION = "0";
    private static final String DOMAIN = "DE";

    @Getter
    private final String code;

    /** Safe client-facing fallback description. */
    @Getter
    private final String description;

    @Getter
    private final String errorLevel;

    @Getter
    private final String type;

    /**
     * Build the complete public error code for an operation.
     */
    public String getCompleteCode(String operationCode) {
        return DOMAIN + VERSION + errorLevel + type + operationCode + code;
    }

    /**
     * Find an error from the three-digit error-specific part of a complete code.
     */
    public static ApplicationErrorCode fromCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (ApplicationErrorCode value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public boolean isInternalError() {
        return this == UNKNOWN || this == SYSTEM;
    }
}
