#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.external;

/**
 * 邮件服务接口
 * @author hanfeng
 */
public interface EmailService {

    /**
     * 发送欢迎邮件
     */
    void sendWelcomeEmail(String email, String username);

    /**
     * 发送密码重置邮件
     */
    void sendPasswordResetEmail(String email, String resetToken);

    /**
     * 发送账户激活邮件
     */
    void sendActivationEmail(String email, String activationToken);
}
