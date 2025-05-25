#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.shared.types;

import java.io.Serializable;

/**
 * 值对象基类
 * @author hanfeng
 */
public interface ValueObject<T> extends Serializable {

    /**
     * 值对象相等性比较
     */
    boolean sameValueAs(T other);
}
