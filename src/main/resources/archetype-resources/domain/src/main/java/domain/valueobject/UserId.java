package ${package}.domain.valueobject;

import lombok.Value;

/**
 * user ID value object
 * @author hanfeng
 */
@Value
public class UserId implements ValueObject<UserId> {

    Long value;

    public UserId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(" user ID ");
        }
        this.value = value;
    }

    public static UserId of(Long value) {
        return new UserId(value);
    }

    @Override
    public boolean sameValueAs(UserId other) {
        return other != null && this.value.equals(other.value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
