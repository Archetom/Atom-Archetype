package ${package}.shared.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * event value
 * each business 0000--9999
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum EventEnum {


    NOT_SUPPORT_EVENT("9999", "NOT_SUPPORT_EVENT", "Unsupported event", "Unsupported event"),

    ;

    /**
     * code
     */
    @Getter
    private final String code;

    /**
     *
     */
    @Getter
    private final String englishName;

    /**
     * in
     */
    @Getter
    private final String chineseName;

    /**
     *
     */
    @Getter
    private final String description;

    /**
     * based on code query.
     *
     * @param code code.
     * @return.
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
