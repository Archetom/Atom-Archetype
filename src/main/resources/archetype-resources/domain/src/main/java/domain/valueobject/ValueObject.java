package ${package}.domain.valueobject;

import java.io.Serializable;

/**
 * value object interface
 * @author hanfeng
 */
public interface ValueObject<T> extends Serializable {

    /**
     * value object etc.
     */
    boolean sameValueAs(T other);

    /**
     * validate value object
     */
    default void validate() {
        // class can override validate
    }
}

