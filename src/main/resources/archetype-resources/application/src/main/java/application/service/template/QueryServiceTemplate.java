package ${package}.application.service.template;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Executes read-only application use cases with consistent result mapping.
 */
@Component
public class QueryServiceTemplate extends OperationTemplateSupport {

    public QueryServiceTemplate(@Value("${spring.application.name}") String appName) {
        super(appName);
    }
}
