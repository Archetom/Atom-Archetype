#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.external;

/**
 * 短信服务接口
 * @author hanfeng
 */
public interface SmsService {

    /**
     * 发送验证码
     */
    void sendVerificationCode(String phoneNumber, String code);

    /**
     * 发送通知短信
     */
    void sendNotification(String phoneNumber, String message);
}
