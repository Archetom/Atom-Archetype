package ${package}.application.service.template;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * Executes state-changing use cases and preserves transaction rollback semantics
 * when failures are converted to API results.
 */
@Component
public class CommandServiceTemplate extends OperationTemplateSupport {

    public CommandServiceTemplate(@Value("${spring.application.name}") String appName) {
        super(appName);
    }

    @Override
    protected void onFailure() {
        try {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (NoTransactionException ignored) {
            // Commands may also be used by callers that deliberately have no transaction.
        }
    }
}
