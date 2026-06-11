#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.application.converter;

import java.util.List;

/**
 * base convert interface
 * @author hanfeng
 */
public interface BaseConverter<S, T> {

    /**
     * single object convert
     */
    T convert(S source);

    /**
     * column table convert
     */
    List<T> convertList(List<S> sourceList);

    /**
     * convert
     */
    S reverse(T target);

    /**
     * column table convert
     */
    List<S> reverseList(List<T> targetList);
}
