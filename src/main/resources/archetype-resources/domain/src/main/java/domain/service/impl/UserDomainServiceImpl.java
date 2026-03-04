package ${package}.domain.service.impl;

import ${package}.domain.entity.User;
import ${package}.domain.exception.UserAlreadyExistsException;
import ${package}.domain.exception.UserDomainException;
import ${package}.domain.repository.UserRepository;
import ${package}.domain.service.UserDomainService;
import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.Username;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * 用户领域服务实现
 * @author hanfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDomainServiceImpl implements UserDomainService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public boolean isUsernameAvailable(Username username) {
        return !userRepository.existsByUsername(username.getValue());
    }

    @Override
    public boolean isEmailAvailable(Email email) {
        return !userRepository.existsByEmail(email.getValue());
    }

    @Override
    public void validateUserCreation(String username, String email) {
        Username usernameVO = new Username(username);
        Email emailVO = new Email(email);

        if (!isUsernameAvailable(usernameVO)) {
            throw new UserAlreadyExistsException(username);
        }

        if (!isEmailAvailable(emailVO)) {
            throw UserAlreadyExistsException.byEmail(email);
        }
    }

    @Override
    public String encryptPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new UserDomainException("密码不能为空");
        }

        return passwordEncoder.encode(plainPassword);
    }

    @Override
    public boolean validatePassword(String plainPassword, String encryptedPassword) {
        try {
            return passwordEncoder.matches(plainPassword, encryptedPassword);
        } catch (Exception e) {
            log.error("密码验证失败", e);
            return false;
        }
    }

    @Override
    public boolean canDeleteUser(User user) {
        if (user == null) {
            return false;
        }

        // 已删除的用户不能再删除
        if (user.isDeleted()) {
            return false;
        }

        // 管理员用户不能删除
        if (user.isAdmin()) {
            log.warn("尝试删除管理员用户: {}", user.getUsernameValue());
            return false;
        }

        // 外部系统用户需要特殊处理
        if (user.isExternalUser()) {
            log.info("删除外部系统用户: {}", user.getUsernameValue());
            // 可以添加额外的验证逻辑
        }

        return true;
    }

    @Override
    public String generateDefaultPassword() {
        // 生成8位随机密码，包含大小写字母和数字
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }

        return password.toString();
    }

    @Override
    public boolean hasPermission(User user, String permission) {
        if (user == null || !user.isActive()) {
            return false;
        }

        // 管理员拥有所有权限
        if (user.isAdmin()) {
            return true;
        }

        // 根据权限类型进行判断
        return switch (permission) {
            case "READ_USER" -> true; // 所有激活用户都可以读取
            case "WRITE_USER" -> user.isAdmin(); // 只有管理员可以写入
            case "DELETE_USER" -> user.isAdmin(); // 只有管理员可以删除
            default -> false;
        };
    }
}
