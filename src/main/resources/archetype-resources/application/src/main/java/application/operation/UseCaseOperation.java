package ${package}.application.operation;

import ${package}.shared.operation.OperationCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/** Stable four-digit identifiers for application operations and public error codes. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum UseCaseOperation implements OperationCode {

    USER_CREATE("1000", "创建用户", "Create a user"),
    USER_GET("1001", "查询用户详情", "Get a user by ID"),
    USER_QUERY("1002", "分页查询用户", "Query users"),
    USER_STATUS_UPDATE("1003", "更新用户状态", "Update user status"),
    USER_DELETE("1004", "删除用户", "Delete a user"),

    ;

    @Getter
    private final String code;

    @Getter
    private final String displayName;

    @Getter
    private final String description;

    @Override
    public String code() {
        return code;
    }

    /** Find an operation by its stable code. */
    public static Optional<UseCaseOperation> findByCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst();
    }
}
