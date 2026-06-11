package ${package}.domain.policy;

import ${package}.api.enums.UserStatus;
import ${package}.domain.entity.User;
import ${package}.domain.exception.UserDomainException;
import org.springframework.stereotype.Component;

/**
 * user status policy
 * @author hanfeng
 */
@Component
public class UserStatusPolicy {

    /**
     * check status whether
     */
    public void validateStatusChange(User user, UserStatus newStatus) {
        if (user == null) {
            throw new UserDomainException("User must not be empty");
        }

        UserStatus currentStatus = user.getStatus();

        // Deleted users cannot change status
        if (currentStatus == UserStatus.DELETED) {
            throw new UserDomainException("Deleted users cannot change status");
        }

        // define status rule
        switch (currentStatus) {
            case ACTIVE:
                if (newStatus == UserStatus.INACTIVE) {
                    throw new UserDomainException("Active users cannot be set directly to inactive");
                }
                break;
            case LOCKED:
                if (newStatus == UserStatus.INACTIVE) {
                    throw new UserDomainException("Locked users cannot be set to inactive");
                }
                break;
            default:
                // status
                break;
        }
    }
}
