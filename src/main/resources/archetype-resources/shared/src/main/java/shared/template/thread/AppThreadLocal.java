package ${package}.shared.template.thread;

/**
 * business
 */
public class AppThreadLocal {

    /**
     * context
     */
    private static final ThreadLocal<AppContext> LOCAL_CONTEXT = new ThreadLocal<>();

    /**
     * initialize context
     */
    public static AppContext init() {
        AppContext context = AppContext.getInstance();
        LOCAL_CONTEXT.set(context);
        return context;
    }

    /**
     * to context
     */
    public static AppContext get() {
        return LOCAL_CONTEXT.get();
    }

    /**
     * to context, if then set, in process of and set LogKey
     */
    public static void set(AppContext context) {
        if (context == null) {
            context = AppContext.getInstance();
        }
        LOCAL_CONTEXT.set(context);
    }

    /**
     * context and clear LogKey
     */
    public static void clear() {
        LOCAL_CONTEXT.remove();
    }
}
