package ${package}.domain.valueobject;

import java.io.Serializable;

/**
 * 值对象基接口
 * @author hanfeng
 */
public interface ValueObject<T> extends Serializable {

    /**
     * 值对象相等性比较
     */
    boolean sameValueAs(T other);

    /**
     * 验证值对象
     */
    default void validate() {
        // 子类可重写进行验证
    }
}

