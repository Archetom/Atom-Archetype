#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.application.converter;

import java.util.List;

/**
 * 基础转换器接口
 * @author hanfeng
 */
public interface BaseConverter<S, T> {

    /**
     * 单个对象转换
     */
    T convert(S source);

    /**
     * 列表转换
     */
    List<T> convertList(List<S> sourceList);

    /**
     * 反向转换
     */
    S reverse(T target);

    /**
     * 反向列表转换
     */
    List<S> reverseList(List<T> targetList);
}
