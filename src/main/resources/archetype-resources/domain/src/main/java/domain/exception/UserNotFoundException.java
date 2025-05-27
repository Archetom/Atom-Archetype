package ${package}.domain.exception;

/**
 * 用户未找到异常
 * @author hanfeng
 */
public class UserNotFoundException extends UserDomainException {

    public UserNotFoundException(Long userId) {
        super("用户不存在: " + userId);
    }

    public UserNotFoundException(String username) {
        super("用户不存在: " + username);
    }
}