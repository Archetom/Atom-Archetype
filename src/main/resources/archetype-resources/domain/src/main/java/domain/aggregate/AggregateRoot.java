package ${package}.domain.aggregate;

import ${package}.domain.event.DomainEvent;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 聚合根基类
 * @author hanfeng
 */
public abstract class AggregateRoot<ID> {

    @Getter
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * 获取聚合根ID
     */
    public abstract ID getId();

    /**
     * 获取领域事件（只读）
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * 清除领域事件
     */
    public void clearDomainEvents() {
        domainEvents.clear();
    }

    /**
     * 添加领域事件
     */
    protected void addDomainEvent(DomainEvent event) {
        if (event != null) {
            domainEvents.add(event);
        }
    }

    /**
     * 移除领域事件
     */
    protected void removeDomainEvent(DomainEvent event) {
        domainEvents.remove(event);
    }

    /**
     * 检查是否有领域事件
     */
    public boolean hasDomainEvents() {
        return !domainEvents.isEmpty();
    }
}
