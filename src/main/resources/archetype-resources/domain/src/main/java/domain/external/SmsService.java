#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.external;

/**
 * SMS service interface
 * @author hanfeng
 */
public interface SmsService {

    /**
     * send verification code
     */
    void sendVerificationCode(String phoneNumber, String code);

    /**
     * send notification SMS
     */
    void sendNotification(String phoneNumber, String message);
}
