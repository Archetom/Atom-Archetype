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
 * user event listener
 * @author hanfeng
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final EmailService emailService;
    private final SmsService smsService;

    /**
     * process user create event
     */
    @Async
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        log.info(" process user create event: userId={}, username={}",
                event.getUserId(), event.getUsername());

        try {
            // send email
            emailService.sendWelcomeEmail(event.getEmail(), event.getUsername());

            log.info(" user create event process: userId={}", event.getUserId());
        } catch (Exception e) {
            log.error(" process user create event failure: userId={}", event.getUserId(), e);
        }
    }

    /**
     * process user status event
     */
    @Async
    @EventListener
    public void handleUserStatusChanged(UserStatusChangedEvent event) {
        log.info(" process user status event: userId={}, oldStatus={}, newStatus={}",
                event.getUserId(), event.getOldStatus(), event.getNewStatus());

        try {
            // based on status send notification
            switch (event.getNewStatus()) {
                case LOCKED -> {
                    log.info(" user locked: userId={}, reason={}", event.getUserId(), event.getReason());
                }
                case ACTIVE -> {
                    log.info(" user active: userId={}", event.getUserId());
                }
                case INACTIVE -> {
                    log.info(" user: userId={}", event.getUserId());
                }
                case DELETED -> {
                    log.info(" user delete: userId={}", event.getUserId());
                }
            }

            log.info(" user status event process: userId={}", event.getUserId());
        } catch (Exception e) {
            log.error(" process user status event failure: userId={}", event.getUserId(), e);
        }
    }
}
