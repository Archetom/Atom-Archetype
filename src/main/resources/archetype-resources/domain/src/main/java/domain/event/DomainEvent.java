package ${package}.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 领域事件接口
 * @author hanfeng
 */
public interface DomainEvent {

    /**
     * 事件ID
     */
    String getEventId();

    /**
     * 事件类型
     */
    String getEventType();

    /**
     * 发生时间
     */
    LocalDateTime getOccurredOn();

    /**
     * 聚合根ID
     */
    String getAggregateId();

    /**
     * 事件版本
     */
    default Integer getVersion() {
        return 1;
    }
}
