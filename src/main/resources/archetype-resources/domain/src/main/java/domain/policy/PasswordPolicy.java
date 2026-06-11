package ${package}.domain.policy;

import ${package}.domain.exception.UserDomainException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * password policy
 * @author hanfeng
 */
@Component
public class PasswordPolicy {

    private static final Pattern STRONG_PASSWORD = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    /**
     * validate password
     */
    public void validate(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new UserDomainException("Password must not be empty");
        }

        if (password.length() < 6) {
            throw new UserDomainException(" password length cannot in 6 position ");
        }

        if (password.length() > 20) {
            throw new UserDomainException(" password length cannot 20 position ");
        }

        // can based on need password policy
        // if (!STRONG_PASSWORD.matcher(password).matches()) {
        // throw new UserDomainException(" password package letters, digits and ");
        // }
    }

    /**
     * check password
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