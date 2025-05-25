#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.external;

import ${package}.domain.external.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 邮件服务实现
 * @author hanfeng
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Override
    public void sendWelcomeEmail(String email, String username) {
        log.info("发送欢迎邮件到: {}, 用户名: {}", email, username);
        // TODO: 集成真实的邮件服务提供商（如 SendGrid、阿里云邮件推送等）
        simulateEmailSending("welcome", email, username);
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetToken) {
        log.info("发送密码重置邮件到: {}, 重置令牌: {}", email, resetToken);
        // TODO: 集成真实的邮件服务
        simulateEmailSending("password-reset", email, resetToken);
    }

    @Override
    public void sendActivationEmail(String email, String activationToken) {
        log.info("发送账户激活邮件到: {}, 激活令牌: {}", email, activationToken);
        // TODO: 集成真实的邮件服务
        simulateEmailSending("activation", email, activationToken);
    }

    private void simulateEmailSending(String type, String email, String content) {
        try {
            // 模拟邮件发送延迟
            Thread.sleep(100);
            log.info("邮件发送成功 - 类型: {}, 收件人: {}, 内容: {}", type, email, content);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("邮件发送被中断", e);
        }
    }
}
