package ${package}.domain.aggregate;

import ${package}.domain.event.DomainEvent;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * aggregate root class
 * @author hanfeng
 */
public abstract class AggregateRoot<ID> {

    @Getter
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    @Getter
    private Long version = 0L;

    /**
     * get aggregate root ID
     */
    public abstract ID getId();

    /**
     * get domain event ()
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * clear domain event
     */
    public void clearDomainEvents() {
        domainEvents.clear();
    }

    /**
     * add domain event
     */
    protected void addDomainEvent(DomainEvent event) {
        if (event != null) {
            domainEvents.add(event);
        }
    }

    /**
     * remove domain event
     */
    protected void removeDomainEvent(DomainEvent event) {
        domainEvents.remove(event);
    }

    /**
     * check whether domain event
     */
    public boolean hasDomainEvents() {
        return !domainEvents.isEmpty();
    }

    /**
     * (used for)
     */
    public void incrementVersion() {
        this.version++;
    }
}
