#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.external;

import ${package}.domain.external.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * email service implementation
 * @author hanfeng
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Override
    public void sendWelcomeEmail(String email, String username) {
        log.info(" send email to: {}, username: {}", email, username);
        // TODO: integration of email service provide (such as SendGrid, email etc.)
        simulateEmailSending("welcome", email, username);
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetToken) {
        log.info(" send password email to: {}, token: {}", email, resetToken);
        // TODO: integration of email service
        simulateEmailSending("password-reset", email, resetToken);
    }

    @Override
    public void sendActivationEmail(String email, String activationToken) {
        log.info(" send active email to: {}, active token: {}", email, activationToken);
        // TODO: integration of email service
        simulateEmailSending("activation", email, activationToken);
    }

    private void simulateEmailSending(String type, String email, String content) {
        // TODO: as of email send
        log.info("Email sent successfully - class: {},: {}, content: {}", type, email, content);
    }
}
