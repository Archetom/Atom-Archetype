#set( $dollar = '$' )
package ${package}.application.service.template;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Executes read-only application use cases with consistent result mapping.
 */
@Component
public class QueryServiceTemplate extends OperationTemplateSupport {

    private final TransactionTemplate transactionTemplate;

    public QueryServiceTemplate(
            @Value("${dollar}{spring.application.name}") String appName,
            PlatformTransactionManager transactionManager) {
        super(appName);
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        transactionTemplate.setReadOnly(true);
    }

    @Override
    protected <T> T invoke(ServiceOperation<T> operation) {
        return transactionTemplate.execute(status -> super.invoke(operation));
    }
}
