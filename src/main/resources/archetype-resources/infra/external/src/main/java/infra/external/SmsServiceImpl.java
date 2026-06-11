#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.external;

import ${package}.domain.external.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SMS service implementation
 * @author hanfeng
 */
@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Override
    public void sendVerificationCode(String phoneNumber, String code) {
        log.info(" send verification code to: {}, verification code: {}", phoneNumber, code);
        // TODO: integration of SMS service provide (such as SMS, SMS etc.)
        simulateSmsSending("verification", phoneNumber, code);
    }

    @Override
    public void sendNotification(String phoneNumber, String message) {
        log.info(" send notification SMS to: {}, message: {}", phoneNumber, message);
        // TODO: integration of SMS service
        simulateSmsSending("notification", phoneNumber, message);
    }

    private void simulateSmsSending(String type, String phoneNumber, String content) {
        // TODO: as of SMS send
        log.info("SMS sent successfully - class: {}, phone number: {}, content: {}", type, phoneNumber, content);
    }
}
