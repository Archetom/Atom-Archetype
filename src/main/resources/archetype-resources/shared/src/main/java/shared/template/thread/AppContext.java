package ${package}.shared.template.thread;

import lombok.Data;

import java.util.Map;

/**
 * business context -- data
 */
@Data
public final class AppContext {

    /**
     * user ID
     */
    private String accountId;

    /**
     * tenant ID
     */
    private String tenantId;

    /**
     * business data (used for layer layer data)
     */
    private Map<String, Object> extra;

    /**
     * initialize function
     */
    public AppContext() {
    }

    /**
     * initialize
     */
    public static AppContext getInstance() {
        return new AppContext();
    }
}

