#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.entity;

import ${package}.api.enums.UserStatus;
import ${package}.shared.exception.AppException;
import ${package}.shared.enums.ErrorCodeEnum;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户领域实体
 * @author hanfeng
 */
@Data
@Accessors(chain = true)
public class User {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 密码（加密后）
     */
    private String password;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 状态
     */
    private UserStatus status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
    
    /**
     * 创建用户
     */
    public static User create(String username, String email, String password, String realName) {
        validateCreateParams(username, email, password);
        
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password); // 实际应用中需要加密
        user.setRealName(realName);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        
        return user;
    }
    
    /**
     * 更新状态
     */
    public void updateStatus(UserStatus newStatus) {
        if (newStatus == null) {
            throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "用户状态不能为空");
        }
        
        if (this.status == UserStatus.DELETED) {
            throw new AppException(ErrorCodeEnum.NOT_SUPPORT_OPERATE_EXP, "已删除的用户不能修改状态");
        }
        
        this.status = newStatus;
        this.updatedTime = LocalDateTime.now();
    }
    
    /**
     * 删除用户（软删除）
     */
    public void delete() {
        if (this.status == UserStatus.DELETED) {
            throw new AppException(ErrorCodeEnum.NOT_SUPPORT_OPERATE_EXP, "用户已被删除");
        }
        
        this.status = UserStatus.DELETED;
        this.updatedTime = LocalDateTime.now();
    }
    
    /**
     * 检查用户是否激活
     */
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }
    
    /**
     * 验证创建参数
     */
    private static void validateCreateParams(String username, String email, String password) {
        if (StringUtils.isBlank(username)) {
            throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "用户名不能为空");
        }
        if (StringUtils.isBlank(email)) {
            throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "邮箱不能为空");
        }
        if (StringUtils.isBlank(password)) {
            throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "密码不能为空");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "用户名长度必须在3-50个字符之间");
        }
        if (password.length() < 6 || password.length() > 20) {
            throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "密码长度必须在6-20个字符之间");
        }
    }
}
