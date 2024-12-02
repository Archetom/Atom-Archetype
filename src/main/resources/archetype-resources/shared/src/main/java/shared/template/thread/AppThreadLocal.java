package ${package}.shared.template.thread;

/**
 * 业务线程
 */
public class AppThreadLocal {

    /**
     * 线程上下文
     */
    private static final ThreadLocal<AppContext> LOCAL_CONTEXT = new ThreadLocal<>();

    /**
     * 初始化线程上下文
     */
    public static AppContext init() {
        AppContext context = AppContext.getInstance();
        LOCAL_CONTEXT.set(context);
        return context;
    }

    /**
     * 拿到线程上下文
     */
    public static AppContext get() {
        return LOCAL_CONTEXT.get();
    }

    /**
     * 拿到线程上下文 ，如果没有则设置，注意一定要在结束处理的时候进行清空 且设置LogKey
     */
    public static void set(AppContext context) {
        if (context == null) {
            context = AppContext.getInstance();
        }
        LOCAL_CONTEXT.set(context);
    }

    /**
     * 清掉线程上下文 且清除LogKey
     */
    public static void clear() {
        LOCAL_CONTEXT.remove();
    }
}
