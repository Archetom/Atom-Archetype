#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.external;

/**
 * email service interface
 * @author hanfeng
 */
public interface EmailService {

    /**
     * send email
     */
    void sendWelcomeEmail(String email, String username);

    /**
     * send password email
     */
    void sendPasswordResetEmail(String email, String resetToken);

    /**
     * send active email
     */
    void sendActivationEmail(String email, String activationToken);
}
