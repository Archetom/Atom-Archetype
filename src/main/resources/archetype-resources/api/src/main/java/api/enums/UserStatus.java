#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举
 * @author hanfeng
 */
@Getter
@AllArgsConstructor
public enum UserStatus {
    
    ACTIVE("ACTIVE", "激活"),
    INACTIVE("INACTIVE", "未激活"),
    LOCKED("LOCKED", "锁定"),
    DELETED("DELETED", "已删除");
    
    private final String code;
    private final String name;
    
    public static UserStatus fromCode(String code) {
        for (UserStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown user status code: " + code);
    }
}
