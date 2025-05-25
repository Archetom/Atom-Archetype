#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.service;

import ${package}.domain.entity.User;

/**
 * 用户领域服务接口
 * @author hanfeng
 */
public interface UserDomainService {
    
    /**
     * 检查用户名是否可用
     */
    boolean isUsernameAvailable(String username);
    
    /**
     * 检查邮箱是否可用
     */
    boolean isEmailAvailable(String email);
    
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
}
