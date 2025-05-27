package ${package}.domain.valueobject;

import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * 用户名值对象
 * @author hanfeng
 */
@Value
public class Username implements ValueObject<Username> {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,50}$");

    String value;

    public Username(String value) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (!USERNAME_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("用户名只能包含字母、数字和下划线，长度3-50位");
        }
        this.value = value;
    }

    public static Username of(String value) {
        return new Username(value);
    }

    @Override
    public boolean sameValueAs(Username other) {
        return other != null && this.value.equals(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
