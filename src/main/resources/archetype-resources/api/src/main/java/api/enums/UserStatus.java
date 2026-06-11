#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * user status
 * @author hanfeng
 */
@Getter
@AllArgsConstructor
public enum UserStatus {
    
    ACTIVE("ACTIVE", " active "),
    INACTIVE("INACTIVE", " inactive "),
    LOCKED("LOCKED", " locked "),
    DELETED("DELETED", " deleted ");
    
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
