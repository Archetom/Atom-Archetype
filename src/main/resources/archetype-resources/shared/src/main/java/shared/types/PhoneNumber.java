#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.shared.types;

import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * 手机号值对象
 * @author hanfeng
 */
@Value
public class PhoneNumber implements ValueObject<PhoneNumber> {

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^1[3-9]\\d{9}$"
    );

    String value;

    public PhoneNumber(String value) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("Phone number cannot be blank");
        }
        if (!PHONE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid phone number format: " + value);
        }
        this.value = value;
    }

    @Override
    public boolean sameValueAs(PhoneNumber other) {
        return other != null && this.value.equals(other.value);
    }

    /**
     * 获取脱敏后的手机号
     */
    public String getMasked() {
        if (value.length() >= 11) {
            return value.substring(0, 3) + "****" + value.substring(7);
        }
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
