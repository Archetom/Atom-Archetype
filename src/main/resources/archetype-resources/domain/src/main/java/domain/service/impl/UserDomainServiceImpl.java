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
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 用户领域服务实现
 * @author hanfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDomainServiceImpl implements UserDomainService {

    private final UserRepository userRepository;
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
        try {
            // 生成盐值
            byte[] salt = new byte[16];
            secureRandom.nextBytes(salt);

            // 使用 SHA-256 + 盐值加密
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(plainPassword.getBytes());

            // 将盐值和哈希值组合
            byte[] combined = new byte[salt.length + hash.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hash, 0, combined, salt.length, hash.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (NoSuchAlgorithmException e) {
            log.error("密码加密失败", e);
            throw new UserDomainException("密码加密失败");
        }
    }

    @Override
    public boolean validatePassword(String plainPassword, String encryptedPassword) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedPassword);

            // 提取盐值
            byte[] salt = new byte[16];
            System.arraycopy(combined, 0, salt, 0, 16);

            // 提取哈希值
            byte[] hash = new byte[combined.length - 16];
            System.arraycopy(combined, 16, hash, 0, hash.length);

            // 使用相同的盐值加密输入密码
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] inputHash = md.digest(plainPassword.getBytes());

            // 比较哈希值
            return MessageDigest.isEqual(hash, inputHash);
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
