#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.messaging;

import ${package}.shared.event.BaseEvent;
import lombok.Getter;

/**
 * 用户创建事件
 * @author hanfeng
 */
@Getter
public class UserCreatedEvent extends BaseEvent {
    
    private final Long userId;
    private final String username;
    private final String email;
    
    public UserCreatedEvent(Object source, Long userId, String username, String email) {
        super(source);
        this.userId = userId;
        this.username = username;
        this.email = email;
    }
}
