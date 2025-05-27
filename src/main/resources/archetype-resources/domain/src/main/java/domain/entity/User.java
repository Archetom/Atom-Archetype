package ${package}.domain.entity;

import ${package}.api.enums.UserStatus;
import ${package}.domain.aggregate.AggregateRoot;
import ${package}.domain.event.UserCreatedEvent;
import ${package}.domain.event.UserStatusChangedEvent;
import ${package}.domain.exception.UserDomainException;
import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.PhoneNumber;
import ${package}.domain.valueobject.UserId;
import ${package}.domain.valueobject.Username;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户聚合根
 * @author hanfeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class User extends AggregateRoot<UserId> {

    private UserId id;
    private Username username;
    private Email email;
    private PhoneNumber phoneNumber;
    private String password;
    private String realName;
    private UserStatus status;
    private Long tenantId;
    private String externalId; // 外部系统ID
    private boolean externalUser; // 是否外部用户
    private boolean admin; // 是否管理员
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    // 无参构造函数 - 仅供框架使用（MapStruct、JPA等）
    public User() {
        // 框架专用构造函数，不进行业务验证
    }

    // ========== 重写 Lombok 生成的 getter 方法以确保类型正确 ==========

    /**
     * 获取用户名值对象
     */
    public Username getUsername() {
        return this.username;
    }

    /**
     * 获取邮箱值对象
     */
    public Email getEmail() {
        return this.email;
    }

    /**
     * 获取手机号值对象
     */
    public PhoneNumber getPhoneNumber() {
        return this.phoneNumber;
    }

    /**
     * 获取用户ID值对象
     */
    @Override
    public UserId getId() {
        return this.id;
    }

    // ========== 便捷方法获取字符串值 ==========

    /**
     * 获取用户名字符串值
     */
    public String getUsernameValue() {
        return username != null ? username.getValue() : null;
    }

    /**
     * 获取邮箱字符串值
     */
    public String getEmailValue() {
        return email != null ? email.getValue() : null;
    }

    /**
     * 获取手机号字符串值
     */
    public String getPhoneNumberValue() {
        return phoneNumber != null ? phoneNumber.getValue() : null;
    }

    /**
     * 获取脱敏手机号
     */
    public String getMaskedPhoneNumber() {
        return phoneNumber != null ? phoneNumber.getMasked() : null;
    }

    // ========== 工厂方法 ==========

    /**
     * 使用值对象创建用户（工厂方法）
     */
    public static User createWithValueObjects(Username username, Email email, String password, String realName) {
        validateCreateParams(username, email, password);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRealName(realName);
        user.setStatus(UserStatus.ACTIVE);
        user.setExternalUser(false);
        user.setAdmin(false);
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());

        // 添加领域事件
        user.addDomainEvent(new UserCreatedEvent(
                user.getId() != null ? user.getId().getValue() : null,
                username.getValue(),
                email.getValue()
        ));

        return user;
    }

    /**
     * 创建用户（工厂方法）
     */
    public static User create(String username, String email, String password, String realName) {
        validateCreateParams(username, email, password);

        User user = new User();
        user.setUsername(new Username(username));
        user.setEmail(new Email(email));
        user.setPassword(password);
        user.setRealName(realName);
        user.setStatus(UserStatus.ACTIVE);
        user.setExternalUser(false);
        user.setAdmin(false);
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());

        // 添加领域事件
        user.addDomainEvent(new UserCreatedEvent(user.getId() != null ? user.getId().getValue() : null, username, email));

        return user;
    }

    /**
     * 创建用户（带手机号）
     */
    public static User create(String username, String email, String phoneNumber, String password, String realName) {
        User user = create(username, email, password, realName);
        if (StringUtils.isNotBlank(phoneNumber)) {
            user.setPhoneNumber(new PhoneNumber(phoneNumber));
        }
        return user;
    }

    // ========== 业务方法 ==========

    /**
     * 更新状态
     */
    public void changeStatus(UserStatus newStatus, String reason) {
        if (newStatus == null) {
            throw new UserDomainException("用户状态不能为空");
        }

        if (this.status == UserStatus.DELETED) {
            throw new UserDomainException("已删除的用户不能修改状态");
        }

        if (this.status == newStatus) {
            return; // 状态未变化，无需处理
        }

        UserStatus oldStatus = this.status;
        this.status = newStatus;
        this.updatedTime = LocalDateTime.now();

        // 添加状态变更事件
        addDomainEvent(new UserStatusChangedEvent(this.getId() != null ? this.getId().getValue() : null, oldStatus, newStatus, reason));
    }

    /**
     * 激活用户
     */
    public void activate() {
        changeStatus(UserStatus.ACTIVE, "用户激活");
    }

    /**
     * 锁定用户
     */
    public void lock(String reason) {
        changeStatus(UserStatus.LOCKED, reason);
    }

    /**
     * 删除用户（软删除）
     */
    public void delete() {
        changeStatus(UserStatus.DELETED, "用户删除");
    }

    /**
     * 更改邮箱（使用值对象）
     */
    public void changeEmail(Email newEmail) {
        if (newEmail == null) {
            throw new UserDomainException("邮箱不能为空");
        }

        if (!newEmail.sameValueAs(this.email)) {
            this.email = newEmail;
            this.updatedTime = LocalDateTime.now();
        }
    }

    /**
     * 更改邮箱（字符串版本）
     */
    public void changeEmail(String newEmail) {
        this.email = new Email(newEmail);
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 更改手机号（使用值对象）
     */
    public void changePhoneNumber(PhoneNumber newPhoneNumber) {
        if (newPhoneNumber != null && !newPhoneNumber.sameValueAs(this.phoneNumber)) {
            this.phoneNumber = newPhoneNumber;
            this.updatedTime = LocalDateTime.now();
        }
    }

    /**
     * 更新手机号（字符串版本）
     */
    public void changePhoneNumber(String newPhoneNumber) {
        this.phoneNumber = new PhoneNumber(newPhoneNumber);
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 设置外部系统ID
     */
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    /**
     * 标记为外部用户
     */
    public void markAsExternalUser() {
        this.externalUser = true;
    }

    /**
     * 授予管理员角色
     */
    public void grantAdminRole() {
        this.admin = true;
    }

    /**
     * 撤销管理员角色
     */
    public void revokeAdminRole() {
        this.admin = false;
    }

    // ========== 状态检查方法 ==========

    /**
     * 检查用户是否激活
     */
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }

    /**
     * 检查用户是否被锁定
     */
    public boolean isLocked() {
        return UserStatus.LOCKED.equals(this.status);
    }

    /**
     * 检查用户是否被删除
     */
    public boolean isDeleted() {
        return UserStatus.DELETED.equals(this.status);
    }

    /**
     * 检查是否为管理员
     */
    public boolean isAdmin() {
        return this.admin;
    }

    /**
     * 检查是否为外部用户
     */
    public boolean isExternalUser() {
        return this.externalUser;
    }

    // ========== 验证方法 ==========

    /**
     * 验证创建参数
     */
    private static void validateCreateParams(String username, String email, String password) {
        if (StringUtils.isBlank(username)) {
            throw new UserDomainException("用户名不能为空");
        }
        if (StringUtils.isBlank(email)) {
            throw new UserDomainException("邮箱不能为空");
        }
        if (StringUtils.isBlank(password)) {
            throw new UserDomainException("密码不能为空");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new UserDomainException("用户名长度必须在3-50个字符之间");
        }
        if (password.length() < 6 || password.length() > 20) {
            throw new UserDomainException("密码长度必须在6-20个字符之间");
        }
    }

    /**
     * 验证创建参数（值对象版本）
     */
    private static void validateCreateParams(Username username, Email email, String password) {
        if (username == null) {
            throw new UserDomainException("用户名不能为空");
        }
        if (email == null) {
            throw new UserDomainException("邮箱不能为空");
        }
        if (StringUtils.isBlank(password)) {
            throw new UserDomainException("密码不能为空");
        }
        if (password.length() < 6 || password.length() > 20) {
            throw new UserDomainException("密码长度必须在6-20个字符之间");
        }
    }
}
