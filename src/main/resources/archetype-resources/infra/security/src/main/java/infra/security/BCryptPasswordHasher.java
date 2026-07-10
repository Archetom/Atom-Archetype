#set( $dollar = '$' )
package ${package}.infra.security;

import ${package}.domain.service.PasswordHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Spring Security adapter for the domain password-hashing port.
 */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder;

    public BCryptPasswordHasher(
            @Value("${dollar}{atom.security.password.bcrypt-strength:12}") int strength) {
        if (strength < 10 || strength > 16) {
            throw new IllegalArgumentException("BCrypt strength must be between 10 and 16");
        }
        this.encoder = new BCryptPasswordEncoder(strength);
    }

    @Override
    public String hash(String plainText) {
        return encoder.encode(plainText);
    }
}
