package ${package}.domain.policy;

import ${package}.domain.exception.UserDomainException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 密码策略
 * @author hanfeng
 */
@Component
public class PasswordPolicy {

    private static final Pattern STRONG_PASSWORD = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    /**
     * 验证密码
     */
    public void validate(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new UserDomainException("密码不能为空");
        }

        if (password.length() < 6) {
            throw new UserDomainException("密码长度不能少于6位");
        }

        if (password.length() > 20) {
            throw new UserDomainException("密码长度不能超过20位");
        }

        // 可以根据需要启用强密码策略
        // if (!STRONG_PASSWORD.matcher(password).matches()) {
        //     throw new UserDomainException("密码必须包含大小写字母、数字和特殊字符");
        // }
    }

    /**
     * 检查密码强度
     */
    public PasswordStrength checkStrength(String password) {
        if (password == null || password.length() < 6) {
            return PasswordStrength.WEAK;
        }

        if (STRONG_PASSWORD.matcher(password).matches()) {
            return PasswordStrength.STRONG;
        }

        return PasswordStrength.MEDIUM;
    }

    public enum PasswordStrength {
        WEAK, MEDIUM, STRONG
    }
}