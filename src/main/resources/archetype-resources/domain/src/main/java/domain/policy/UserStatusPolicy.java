package ${package}.domain.policy;

import ${package}.api.enums.UserStatus;
import ${package}.domain.entity.User;
import ${package}.domain.exception.UserDomainException;
import org.springframework.stereotype.Component;

/**
 * 用户状态策略
 * @author hanfeng
 */
@Component
public class UserStatusPolicy {

    /**
     * 检查状态变更是否允许
     */
    public void validateStatusChange(User user, UserStatus newStatus) {
        if (user == null) {
            throw new UserDomainException("用户不能为空");
        }

        UserStatus currentStatus = user.getStatus();

        // 已删除的用户不能变更状态
        if (currentStatus == UserStatus.DELETED) {
            throw new UserDomainException("已删除的用户不能修改状态");
        }

        // 定义状态变更规则
        switch (currentStatus) {
            case ACTIVE:
                if (newStatus == UserStatus.INACTIVE) {
                    throw new UserDomainException("激活用户不能直接设置为未激活");
                }
                break;
            case LOCKED:
                if (newStatus == UserStatus.INACTIVE) {
                    throw new UserDomainException("锁定用户不能设置为未激活");
                }
                break;
            default:
                // 其他状态变更允许
                break;
        }
    }
}
