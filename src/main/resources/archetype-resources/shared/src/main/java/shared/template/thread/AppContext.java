package ${package}.shared.template.thread;

import lombok.Data;

import java.util.Map;

/**
 * 业务线程变量上下文--隐式传递数据
 */
@Data
public final class AppContext {

    /**
     * 用户 ID
     */
    private String accountId;

    /**
     * 租户 ID
     */
    private String tenantId;

    /**
     * 业务数据（用于底层向上层数据传递）
     */
    private Map<String, Object> extra;

    /**
     * 初始化函数
     */
    public AppContext() {
    }

    /**
     * 初始化
     */
    public static AppContext getInstance() {
        return new AppContext();
    }
}

