package ${package}.application.event.listener;

import ${package}.domain.event.UserCreatedEvent;
import ${package}.domain.event.UserStatusChangedEvent;
import ${package}.domain.external.EmailService;
import ${package}.domain.external.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 用户事件监听器
 * @author hanfeng
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final EmailService emailService;
    private final SmsService smsService;

    /**
     * 处理用户创建事件
     */
    @Async
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("处理用户创建事件: userId={}, username={}",
                event.getUserId(), event.getUsername());

        try {
            // 发送欢迎邮件
            emailService.sendWelcomeEmail(event.getEmail(), event.getUsername());

            log.info("用户创建事件处理完成: userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("处理用户创建事件失败: userId={}", event.getUserId(), e);
        }
    }

    /**
     * 处理用户状态变更事件
     */
    @Async
    @EventListener
    public void handleUserStatusChanged(UserStatusChangedEvent event) {
        log.info("处理用户状态变更事件: userId={}, oldStatus={}, newStatus={}",
                event.getUserId(), event.getOldStatus(), event.getNewStatus());

        try {
            // 根据状态变更发送通知
            switch (event.getNewStatus()) {
                case LOCKED -> {
                    log.info("用户账户被锁定: userId={}, reason={}", event.getUserId(), event.getReason());
                }
                case ACTIVE -> {
                    log.info("用户账户被激活: userId={}", event.getUserId());
                }
                case INACTIVE -> {
                    log.info("用户账户被停用: userId={}", event.getUserId());
                }
                case DELETED -> {
                    log.info("用户账户被删除: userId={}", event.getUserId());
                }
            }

            log.info("用户状态变更事件处理完成: userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("处理用户状态变更事件失败: userId={}", event.getUserId(), e);
        }
    }
}
