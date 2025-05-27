package ${package}.domain.factory;

import ${package}.domain.entity.User;
import ${package}.domain.policy.PasswordPolicy;
import ${package}.domain.policy.UserCreationPolicy;
import ${package}.domain.service.UserDomainService;
import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.PhoneNumber;
import ${package}.domain.valueobject.Username;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户工厂 - 负责复杂用户对象的创建
 * @author hanfeng
 */
@Component
@RequiredArgsConstructor
public class UserFactory {

    private final UserDomainService userDomainService;
    private final PasswordPolicy passwordPolicy;
    private final UserCreationPolicy userCreationPolicy;

    /**
     * 创建标准用户
     */
    public User createStandardUser(String username, String email, String password, String realName) {
        // 创建值对象
        Username usernameVO = new Username(username);
        Email emailVO = new Email(email);

        // 验证创建策略
        userCreationPolicy.validateCreation(usernameVO, emailVO);

        // 验证业务规则
        userDomainService.validateUserCreation(username, email);

        // 验证密码策略
        passwordPolicy.validate(password);

        // 加密密码
        String encryptedPassword = userDomainService.encryptPassword(password);

        // 使用内部工厂方法创建用户（跳过密码校验）
        User user = User.createWithValidatedParams(usernameVO, emailVO, encryptedPassword, realName);

        return user;
    }

    /**
     * 创建带手机号的用户
     */
    public User createUserWithPhone(String username, String email, String phoneNumber,
                                    String password, String realName) {
        // 创建值对象
        Username usernameVO = new Username(username);
        Email emailVO = new Email(email);
        PhoneNumber phoneVO = new PhoneNumber(phoneNumber);

        // 验证创建策略
        userCreationPolicy.validateCreation(usernameVO, emailVO);

        // 验证业务规则
        userDomainService.validateUserCreation(username, email);

        // 验证密码策略
        passwordPolicy.validate(password);

        // 加密密码
        String encryptedPassword = userDomainService.encryptPassword(password);

        // 使用内部工厂方法创建用户（跳过密码校验）
        User user = User.createWithValidatedParams(usernameVO, emailVO, phoneVO, encryptedPassword, realName);

        return user;
    }

    /**
     * 从外部系统创建用户
     */
    public User createFromExternalSystem(String externalId, String username, String email, String realName) {
        // 生成临时密码
        String tempPassword = userDomainService.generateDefaultPassword();

        User user = createStandardUser(username, email, tempPassword, realName);

        // 设置外部系统标识
        user.setExternalId(externalId);
        user.markAsExternalUser();

        return user;
    }

    /**
     * 创建管理员用户
     */
    public User createAdminUser(String username, String email, String password, String realName) {
        User user = createStandardUser(username, email, password, realName);
        user.grantAdminRole();
        return user;
    }
}
