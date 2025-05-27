package ${package}.domain.service;

import ${package}.domain.entity.User;
import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.Username;

/**
 * 用户领域服务接口
 * @author hanfeng
 */
public interface UserDomainService {

    /**
     * 检查用户名是否可用
     */
    boolean isUsernameAvailable(Username username);

    /**
     * 检查邮箱是否可用
     */
    boolean isEmailAvailable(Email email);

    /**
     * 验证用户创建规则
     */
    void validateUserCreation(String username, String email);

    /**
     * 加密密码
     */
    String encryptPassword(String plainPassword);

    /**
     * 验证密码
     */
    boolean validatePassword(String plainPassword, String encryptedPassword);

    /**
     * 检查用户是否可以删除
     */
    boolean canDeleteUser(User user);

    /**
     * 生成用户默认密码
     */
    String generateDefaultPassword();

    /**
     * 检查用户权限
     */
    boolean hasPermission(User user, String permission);
}
