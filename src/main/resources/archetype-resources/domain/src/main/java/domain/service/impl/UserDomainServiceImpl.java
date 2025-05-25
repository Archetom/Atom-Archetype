#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.service.impl;

import ${package}.domain.entity.User;
import ${package}.domain.repository.UserRepository;
import ${package}.domain.service.UserDomainService;
import ${package}.shared.exception.AppException;
import ${package}.shared.enums.ErrorCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    
    @Override
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }
    
    @Override
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }
    
    @Override
    public void validateUserCreation(String username, String email) {
        if (!isUsernameAvailable(username)) {
            throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "用户名已存在");
        }
        
        if (!isEmailAvailable(email)) {
            throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "邮箱已存在");
        }
    }
    
    @Override
    public String encryptPassword(String plainPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(plainPassword.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("密码加密失败", e);
            throw new AppException(ErrorCodeEnum.SYSTEM_EXP, "密码加密失败");
        }
    }
    
    @Override
    public boolean validatePassword(String plainPassword, String encryptedPassword) {
        String encrypted = encryptPassword(plainPassword);
        return encrypted.equals(encryptedPassword);
    }
}
