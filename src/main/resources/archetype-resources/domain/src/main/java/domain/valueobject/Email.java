#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.valueobject;

import lombok.Value;

import java.util.regex.Pattern;

/**
 * 邮箱值对象
 * @author hanfeng
 */
@Value
public class Email implements ValueObject<Email> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );

    String value;

    public Email(String value) {
        if (value == null || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
        this.value = value;
    }

    @Override
    public boolean sameValueAs(Email other) {
        return other != null && this.value.equals(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
