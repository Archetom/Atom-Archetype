#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.external;

import ${package}.domain.external.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 短信服务实现
 * @author hanfeng
 */
@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Override
    public void sendVerificationCode(String phoneNumber, String code) {
        log.info("发送验证码到: {}, 验证码: {}", phoneNumber, code);
        // TODO: 集成真实的短信服务提供商（如阿里云短信、腾讯云短信等）
        simulateSmsSending("verification", phoneNumber, code);
    }

    @Override
    public void sendNotification(String phoneNumber, String message) {
        log.info("发送通知短信到: {}, 消息: {}", phoneNumber, message);
        // TODO: 集成真实的短信服务
        simulateSmsSending("notification", phoneNumber, message);
    }

    private void simulateSmsSending(String type, String phoneNumber, String content) {
        try {
            // 模拟短信发送延迟
            Thread.sleep(50);
            log.info("短信发送成功 - 类型: {}, 手机号: {}, 内容: {}", type, phoneNumber, content);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("短信发送被中断", e);
        }
    }
}
