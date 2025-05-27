package ${package}.domain.shared;

import ${package}.domain.service.UserDomainService;
import ${package}.domain.policy.PasswordPolicy;
import ${package}.domain.policy.UserStatusPolicy;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 领域注册表 - 提供领域服务的统一访问点
 * @author hanfeng
 */
@Component
@Getter
public class DomainRegistry {

    private static DomainRegistry instance;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private PasswordPolicy passwordPolicy;

    @Autowired
    private UserStatusPolicy userStatusPolicy;

    @Autowired
    public void setInstance() {
        instance = this;
    }

    public static DomainRegistry getInstance() {
        return instance;
    }

    public static UserDomainService userDomainService() {
        return getInstance().getUserDomainService();
    }

    public static PasswordPolicy passwordPolicy() {
        return getInstance().getPasswordPolicy();
    }

    public static UserStatusPolicy userStatusPolicy() {
        return getInstance().getUserStatusPolicy();
    }
}
