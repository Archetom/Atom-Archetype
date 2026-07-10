package ${package}.application.config;

import ${package}.domain.factory.UserFactory;
import ${package}.domain.policy.PasswordPolicy;
import ${package}.domain.repository.UserRepository;
import ${package}.domain.service.PasswordHasher;
import ${package}.domain.service.UserDomainService;
import ${package}.domain.service.impl.UserDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-neutral domain model into the application runtime.
 */
@Configuration(proxyBeanMethods = false)
public class DomainConfiguration {

    @Bean
    PasswordPolicy passwordPolicy() {
        return new PasswordPolicy();
    }

    @Bean
    UserDomainService userDomainService(UserRepository repository, PasswordHasher passwordHasher) {
        return new UserDomainServiceImpl(repository, passwordHasher);
    }

    @Bean
    UserFactory userFactory(UserDomainService domainService,
                            PasswordPolicy passwordPolicy) {
        return new UserFactory(domainService, passwordPolicy);
    }
}
