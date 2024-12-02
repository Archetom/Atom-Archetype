package ${package}.shared.enums;

import ${package}.shared.consts.ErrorLevelConst;
import ${package}.shared.consts.ErrorTypeConst;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码
 *
 * <p>
 * * <p><b>平台内部唯一</b></p>
 * *
 * * <p>
 * * code对应统一错误码14~16位; errorLevel对应统一错误码的第4位（错误级别）; type对应于统一错误码的第5位（错误类型）。
 * * </p>
 * * <p>
 * * 内部错误码的code取值区间暂定如下：
 * * <ul>
 * * <li>未知异常[000]</li>
 * * <li>系统异常[001]</li>
 * * <li>RPC异常[010~099]</li>
 * * <li>请求参数校验异常[100-149]</li>
 * * <li>程序控制异常[150-199]</li>
 * * <li>数据库操作异常[200-299]</li>
 * * <li>业务规则异常[300-999]</li>
 * * </ul>
 * * </p>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ErrorCodeEnum {
    /**
     * 未知异常
     */
    UNKNOWN_EXP("000", "UNKNOWN_EXP", "未知异常", "未知异常", ErrorLevelConst.ERROR, ErrorTypeConst.SYSTEM),

    /**
     * 系统异常
     */
    SYSTEM_EXP("001", "SYSTEM_EXP", "系统异常", "系统异常", ErrorLevelConst.ERROR, ErrorTypeConst.SYSTEM),

    /**
     * 不支持的操作异常
     */
    NOT_SUPPORT_OPERATE_EXP("002", "NOT_SUPPORT_OPERATE_EXP", "不支持的操作异常", "不支持的操作异常",
            ErrorLevelConst.ERROR, ErrorTypeConst.BIZ),

    //********************  RPC异常 [010~099]  ********************

    //********************  请求参数校验异常[100-149]  ********************

    /**
     * 幂等控制异常、重复操作。
     */
    MAIN_TRANS_CONTROL_EXP("100", "MAIN_TRANS_CONTROL_EXP", "幂等控制异常、重复操作", "幂等控制异常、重复操作",
            ErrorLevelConst.WARN, ErrorTypeConst.BIZ),

    /**
     * 参数校验异常
     */
    PARAM_CHECK_EXP("101", "PARAM_CHECK_EXP", "参数校验异常", "参数校验异常", ErrorLevelConst.WARN,
            ErrorTypeConst.BIZ),

    //********************  数据库操作异常[200-299]  ********************

    //********************  业务规则异常[300-499]  **********************

    /**
     * 不支持的支付渠道
     */
    NO_SUPPORT_CHANNEL("301", "NO_SUPPORT_CHANNEL", "不支持的支付渠道", "不支持的支付渠道", ErrorLevelConst.WARN,
            ErrorTypeConst.BIZ),

    //********************  业务规则异常[500-699]  **********************
    ;

    /**
     * 常量代表版本号
     */
    public final static String VERSION = "0";
    /**
     * 常量代表固定标识
     */
    private final static String DE = "DE";

    /**
     * 错误编码
     */
    @Getter
    private final String code;
    /**
     * 错误英文名
     */
    @Getter
    private final String englishName;
    /**
     * 错误中文名
     */
    @Getter
    private final String chineseName;
    /**
     * 错误描述说明
     */
    @Getter
    private final String description;
    /**
     * 错误级别
     */
    @Getter
    private final String errorLevel;
    /**
     * 错误类型
     */
    @Getter
    private final String type;

    /**
     * 根据标准错误码获取标准<code>ErrorCode_C<code>定义
     *
     * @param eventCode 事件编码
     * @return 完整的errorCode
     */
    public String getCompleteCode(String eventCode) {

        return DE + VERSION + errorLevel + type + eventCode + code;
    }
}

