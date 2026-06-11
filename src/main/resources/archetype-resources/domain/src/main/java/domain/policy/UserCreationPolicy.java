package ${package}.domain.policy;

import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.Username;
import org.springframework.stereotype.Component;

/**
 * user create policy
 * @author hanfeng
 */
@Component
public class UserCreationPolicy {

    /**
     * validate user create rule
     */
    public void validateCreation(Username username, Email email) {
        validateUsername(username);
        validateEmail(email);
        validateBusinessRules(username, email);
    }

    private void validateUsername(Username username) {
        // username business rule validate
        String value = username.getValue();

        // check whether package sensitive word
        if (containsSensitiveWords(value)) {
            throw new IllegalArgumentException(" username package sensitive word ");
        }

        // check whether as reserved username
        if (isReservedUsername(value)) {
            throw new IllegalArgumentException(" username as system reserved username ");
        }
    }

    private void validateEmail(Email email) {
        // email business rule validate
        String value = email.getValue();

        // check whether as corporate email (if)
        if (requiresCorporateEmail() && !isCorporateEmail(value)) {
            throw new IllegalArgumentException(" corporate email ");
        }
    }

    private void validateBusinessRules(Username username, Email email) {
        // TODO: add business rule validate
    }

    private boolean containsSensitiveWords(String username) {
        // TODO: based on business need configuration sensitive word column table (from configuration in or database)
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
        // TODO: based on business need configuration reserved username column table
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
        // TODO: based on configuration or business rule whether need corporate email
        return false;
    }

    private boolean isCorporateEmail(String email) {
        // TODO: based on business need configuration email column table
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
