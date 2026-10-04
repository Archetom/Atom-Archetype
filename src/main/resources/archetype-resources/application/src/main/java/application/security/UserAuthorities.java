package ${package}.application.security;

/**
 * Authorities required by the User use cases.
 *
 * <p>Use cases check them through {@link CallerGuard}; HTTP route rules and token scopes use
 * the same values, so define each capability once here.</p>
 */
public final class UserAuthorities {

    public static final String READ = "users:read";
    public static final String WRITE = "users:write";
    public static final String DELETE = "users:delete";

    private UserAuthorities() {
    }
}
