package ${package}.domain.policy;

import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.Username;
import org.springframework.stereotype.Component;

/**
 * 用户创建策略
 * @author hanfeng
 */
@Component
public class UserCreationPolicy {

    /**
     * 验证用户创建规则
     */
    public void validateCreation(Username username, Email email) {
        validateUsername(username);
        validateEmail(email);
        validateBusinessRules(username, email);
    }

    private void validateUsername(Username username) {
        // 用户名业务规则验证
        String value = username.getValue();

        // 检查是否包含敏感词
        if (containsSensitiveWords(value)) {
            throw new IllegalArgumentException("用户名包含敏感词汇");
        }

        // 检查是否为保留用户名
        if (isReservedUsername(value)) {
            throw new IllegalArgumentException("该用户名为系统保留用户名");
        }
    }

    private void validateEmail(Email email) {
        // 邮箱业务规则验证
        String value = email.getValue();

        // 检查是否为企业邮箱（如果有要求）
        if (requiresCorporateEmail() && !isCorporateEmail(value)) {
            throw new IllegalArgumentException("必须使用企业邮箱注册");
        }
    }

    private void validateBusinessRules(Username username, Email email) {
        // TODO: 添加其他业务规则验证
    }

    private boolean containsSensitiveWords(String username) {
        // TODO: 根据业务需要配置敏感词列表（建议从配置中心或数据库加载）
        String[] sensitiveWords = {};
        String lowerUsername = username.toLowerCase();

        for (String word : sensitiveWords) {
            if (lowerUsername.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean isReservedUsername(String username) {
        // TODO: 根据业务需要配置保留用户名列表
        String[] reserved = {};
        String lowerUsername = username.toLowerCase();

        for (String word : reserved) {
            if (lowerUsername.equals(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean requiresCorporateEmail() {
        // TODO: 根据配置或业务规则决定是否需要企业邮箱
        return false;
    }

    private boolean isCorporateEmail(String email) {
        // TODO: 根据业务需要配置个人邮箱域名列表
        String[] personalDomains = {};
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();

        for (String personalDomain : personalDomains) {
            if (domain.equals(personalDomain)) {
                return false;
            }
        }
        return true;
    }
}
