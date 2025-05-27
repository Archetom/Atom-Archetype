package ${package}.domain.exception;

/**
 * 用户已存在异常
 * @author hanfeng
 */
public class UserAlreadyExistsException extends UserDomainException {

    public UserAlreadyExistsException(String username) {
        super("用户名已存在: " + username);
    }

    public static UserAlreadyExistsException byEmail(String email) {
        return new UserAlreadyExistsException("邮箱已存在: " + email);
    }
}
