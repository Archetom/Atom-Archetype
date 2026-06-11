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
 * user domain service implementation
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
            throw new UserDomainException("Password must not be empty");
        }

        return passwordEncoder.encode(plainPassword);
    }

    @Override
    public boolean validatePassword(String plainPassword, String encryptedPassword) {
        try {
            return passwordEncoder.matches(plainPassword, encryptedPassword);
        } catch (Exception e) {
            log.error(" password validate failure ", e);
            return false;
        }
    }

    @Override
    public boolean canDeleteUser(User user) {
        if (user == null) {
            return false;
        }

        // Deleted users cannot be deleted again
        if (user.isDeleted()) {
            return false;
        }

        // Administrator users cannot be deleted
        if (user.isAdmin()) {
            log.warn(" try delete administrator user: {}", user.getUsernameValue());
            return false;
        }

        // external system user need process
        if (user.isExternalUser()) {
            log.info(" delete external system user: {}", user.getUsernameValue());
            // can add of validate
        }

        return true;
    }

    @Override
    public String generateDefaultPassword() {
        // generate 8 position password, package letters and digits
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

        // administrator all permission
        if (user.isAdmin()) {
            return true;
        }

        // based on permission class
        return switch (permission) {
            case "READ_USER" -> true; // all active user can
            case "WRITE_USER" -> user.isAdmin(); // administrator can
            case "DELETE_USER" -> user.isAdmin(); // administrator can delete
            default -> false;
        };
    }
}
