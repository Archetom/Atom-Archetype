package ${package}.shared.enums;

import ${package}.shared.consts.ErrorLevelConst;
import ${package}.shared.consts.ErrorTypeConst;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * error code
 *
 * <p>
 * * <p><b> internal </b></p>
 * *
 * * <p>
 * * code for unified error code 14~16 position; errorLevel for unified error code of No. 4 position (error level); type for in unified error code of No. 5 position (error type).
 * * </p>
 * * <p>
 * * internal error code of code value such as:
 * * <ul>
 * * <li> unknown exception [000]</li>
 * * <li> system exception [001]</li>
 * * <li>RPC exception [010~099]</li>
 * * <li> request parameter validation exception [100-149]</li>
 * * <li> control exception [150-199]</li>
 * * <li> database exception [200-299]</li>
 * * <li> business rule exception [300-999]</li>
 * * </ul>
 * * </p>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ErrorCodeEnum {
    /**
     * unknown exception
     */
    UNKNOWN_EXP("000", "UNKNOWN_EXP", " unknown exception ", " unknown exception ", ErrorLevelConst.ERROR, ErrorTypeConst.SYSTEM),

    /**
     * system exception
     */
    SYSTEM_EXP("001", "SYSTEM_EXP", " system exception ", " system exception ", ErrorLevelConst.ERROR, ErrorTypeConst.SYSTEM),

    /**
     * Unsupported operation exception
     */
    NOT_SUPPORT_OPERATE_EXP("002", "NOT_SUPPORT_OPERATE_EXP", "Unsupported operation exception", "Unsupported operation exception",
            ErrorLevelConst.ERROR, ErrorTypeConst.BIZ),

    //******************** RPC exception [010~099] ********************

    //******************** request parameter validation exception [100-149] ********************

    /**
     * idempotency control exception,.
     */
    MAIN_TRANS_CONTROL_EXP("100", "MAIN_TRANS_CONTROL_EXP", " idempotency control exception, ", " idempotency control exception, ",
            ErrorLevelConst.WARN, ErrorTypeConst.BIZ),

    /**
     * parameter validation exception
     */
    PARAM_CHECK_EXP("101", "PARAM_CHECK_EXP", " parameter validation exception ", " parameter validation exception ", ErrorLevelConst.WARN,
            ErrorTypeConst.BIZ),

    //******************** database exception [200-299] ********************

    //******************** business rule exception [300-499] **********************

    /**
     * unsupported of
     */
    NO_SUPPORT_CHANNEL("301", "NO_SUPPORT_CHANNEL", " unsupported of ", " unsupported of ", ErrorLevelConst.WARN,
            ErrorTypeConst.BIZ),

    //******************** business rule exception [500-699] **********************
    ;

    /**
     * table
     */
    public final static String VERSION = "0";
    /**
     * table
     */
    private final static String DE = "DE";

    /**
     * error code
     */
    @Getter
    private final String code;
    /**
     * error
     */
    @Getter
    private final String englishName;
    /**
     * error in
     */
    @Getter
    private final String chineseName;
    /**
     * error
     */
    @Getter
    private final String description;
    /**
     * error level
     */
    @Getter
    private final String errorLevel;
    /**
     * error type
     */
    @Getter
    private final String type;

    /**
     * based on standard error code get standard <code>ErrorCode_C<code> define
     *
     * @param eventCode event code
     * @return full of errorCode
     */
    public String getCompleteCode(String eventCode) {

        return DE + VERSION + errorLevel + type + eventCode + code;
    }
}

