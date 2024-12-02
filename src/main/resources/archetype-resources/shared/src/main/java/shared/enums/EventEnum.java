package ${package}.shared.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 事件枚举值
 * 每个业务独立申请  0000--9999
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum EventEnum {


    NOT_SUPPORT_EVENT("9999", "NOT_SUPPORT_EVENT", "不支持的事件", "不支持的事件"),

    ;

    /**
     * 枚举编码
     */
    @Getter
    private final String code;

    /**
     * 英文名
     */
    @Getter
    private final String englishName;

    /**
     * 中文名
     */
    @Getter
    private final String chineseName;

    /**
     * 枚举描述信息
     */
    @Getter
    private final String description;

    /**
     * 根据编码查询枚举。
     *
     * @param code 编码。
     * @return 枚举。
     */
    public static EventEnum getByCode(String code) {
        for (EventEnum value : EventEnum.values()) {
            if (Objects.equals(code, value.getCode())) {
                return value;
            }
        }
        return null;
    }
}
