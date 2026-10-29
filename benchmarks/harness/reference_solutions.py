"""Evaluator-only calibration implementations; never copy this file into an agent workspace.

These small patches prove the acceptance tests distinguish working changes from
representative semantic defects. They are not an agent adapter or model output.
"""
from pathlib import Path
import runpy

PACKAGE = "com.example.benchmark"
PACKAGE_PATH = PACKAGE.replace(".", "/")


def _java(module, relative):
    return f"{module}/src/main/java/{PACKAGE_PATH}/{relative}"


def _replace(project, relative, old, new):
    path = project / relative
    text = path.read_text(encoding="utf-8")
    if text.count(old) != 1:
        raise ValueError(f"Reference fixture no longer matches baseline: {relative}")
    path.write_text(text.replace(old, new), encoding="utf-8")


def _write(project, relative, content):
    path = project / relative
    if path.exists():
        raise ValueError(f"Reference fixture would overwrite an existing file: {relative}")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def apply(project, scenario, variant="correct"):
    """Apply one fixture to a fresh generated copy. Caller owns disposable isolation."""
    project = Path(project).resolve()
    if variant not in {"correct", "mutant"}:
        raise ValueError("variant must be correct or mutant")
    if not (project / "pom.xml").is_file() or (project / "src/main/resources/archetype-resources").exists():
        raise ValueError("Reference patches require a disposable generated application")
    implementations = {"add-api": _add_api, "domain-invariant": _domain_invariant,
                       "persistence-change": _persistence_change}
    if scenario not in implementations:
        raise ValueError(f"Unknown scenario: {scenario}")
    implementations[scenario](project, variant)


def _domain_invariant(project, variant):
    if variant == "mutant":
        # Typical defect: leave the restriction in documentation or an HTTP adapter only.
        return
    path = _java("domain", "domain/entity/User.java")
    _replace(project, path, "        if (this.status == newStatus) {", '''        if (this.status == UserStatus.LOCKED && newStatus == UserStatus.ACTIVE) {
            throw new UserDomainException("Locked users must pass through INACTIVE before activation");
        }

        if (this.status == newStatus) {''')


def _add_api(project, variant):
    # An ordinary candidate must also keep its existing test double compilable.
    runpy.run_path(str(Path(__file__).parent / "acceptance" / "compatibility.py"))["apply"](project, "add-api")
    _write(project, _java("api", "api/dto/request/UserEmailUpdateRequest.java"), f'''package {PACKAGE}.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserEmailUpdateRequest(@NotBlank @Email @Size(max = 254) String email) {{
}}
''')
    _replace(project, _java("api", "api/facade/UserFacade.java"),
             "    /** Soft-delete a user through the deletion-specific use case. */",
             "    Result<Void> updateUserEmail(Long userId, String email);\n\n    /** Soft-delete a user through the deletion-specific use case. */")
    _replace(project, _java("application", "application/service/UserService.java"),
             "    /** Soft-delete a user through the deletion-specific use case. */",
             "    Result<Void> updateUserEmail(AuthenticatedCaller caller, Long userId, String email);\n\n    /** Soft-delete a user through the deletion-specific use case. */")
    _replace(project, _java("application", "application/operation/UseCaseOperation.java"),
             '    USER_DELETE("1004", "删除用户", "Delete a user"),',
             '    USER_DELETE("1004", "删除用户", "Delete a user"),\n    USER_EMAIL_UPDATE("1005", "修改邮箱", "Update user email"),')
    service_path = _java("application", "application/service/impl/UserServiceImpl.java")
    guard = "tenantId = callerGuard.requireTenant(caller, UserAuthorities.WRITE);"
    if variant == "mutant":
        # Compiles and persists, but permits read-only actors both at HTTP and use-case boundaries.
        guard = "tenantId = new TenantId(caller.tenantId());"
        _replace(project, _java("infra/rest", "infra/rest/security/UserRouteAuthorization.java"),
                 ".requestMatchers(HttpMethod.PUT, USER_ROUTES).hasAuthority(UserAuthorities.WRITE)",
                 ".requestMatchers(HttpMethod.PUT, USER_ROUTES).authenticated()")
    method = '''    @Override
    public Result<Void> updateUserEmail(AuthenticatedCaller caller, Long userId, String email) {
        return commandTemplate.execute(UseCaseOperation.USER_EMAIL_UPDATE, new ServiceOperation<Void>() {
            private TenantId tenantId;
            private UserId id;
            private EMAIL_TYPE validatedEmail;

            @Override
            public void validate() {
                GUARD
                id = requireUserId(userId);
            }

            @Override
            public void prepare() {
                validatedEmail = new EMAIL_TYPE(email);
            }

            @Override
            public Void execute() {
                User user = findVisibleUser(tenantId, id);
                user.changeEmail(validatedEmail);
                userRepository.save(tenantId, user);
                return null;
            }

            @Override
            public void onSuccess(Void ignored) {
                afterCommitExecutor.execute(() -> userCacheService.invalidateUser(tenantId, id));
            }
        });
    }

'''.replace("EMAIL_TYPE", PACKAGE + ".domain.valueobject.Email").replace("GUARD", guard)
    _replace(project, service_path, "    private UserId requireUserId(Long userId) {", method + "    private UserId requireUserId(Long userId) {")
    _replace(project, _java("infra/facade", "infra/facade/UserFacadeImpl.java"),
             "    private AuthenticatedCaller currentCaller() {", '''    @Override
    public Result<Void> updateUserEmail(Long userId, String email) {
        return userService.updateUserEmail(currentCaller(), userId, email);
    }

    private AuthenticatedCaller currentCaller() {''')
    _replace(project, _java("infra/rest", "infra/rest/controller/UserController.java"),
             '    @Operation(summary = "Create a user")', f'''    @Operation(summary = "Update a user's email")
    @PutMapping("/{{userId}}/email")
    public ResponseEntity<?> updateUserEmail(@PathVariable Long userId,
            @Valid @RequestBody {PACKAGE}.api.dto.request.UserEmailUpdateRequest request) {{
        return ResponseEntityUtil.assembleResponse(userFacade.updateUserEmail(userId, request.email()));
    }}

    @Operation(summary = "Create a user")''')


def _persistence_change(project, variant):
    _write(project, "infra/persistence/src/main/resources/db/migration/V2__add_preferred_language.sql",
           "ALTER TABLE t_user ADD COLUMN preferred_language VARCHAR(10) NOT NULL DEFAULT 'en';\n")
    user_path = _java("domain", "domain/entity/User.java")
    _replace(project, user_path, "    private String realName;", "    private String realName;\n    private String preferredLanguage = \"en\";")
    old_snapshot_end = '''            LocalDateTime updatedTime
    ) {
    }'''
    new_snapshot_end = '''            LocalDateTime updatedTime,
            String preferredLanguage
    ) {
        public UserSnapshot(UserId id, Username username, Email email, PhoneNumber phoneNumber,
                            PasswordHash passwordHash, String realName, UserStatus status,
                            TenantId tenantId, String externalId, boolean externalUser, boolean admin,
                            Long version, LocalDateTime createdTime, LocalDateTime updatedTime) {
            this(id, username, email, phoneNumber, passwordHash, realName, status, tenantId,
                    externalId, externalUser, admin, version, createdTime, updatedTime, "en");
        }
    }'''
    _replace(project, user_path, old_snapshot_end, new_snapshot_end)
    restored = 'requirePreferredLanguage(snapshot.preferredLanguage())' if variant == "correct" else '"en"'
    # Mutant represents forgetting to map the new persisted field during reconstruction.
    _replace(project, user_path, "        user.realName = snapshot.realName();",
             "        user.realName = snapshot.realName();\n        user.preferredLanguage = " + restored + ";")
    _replace(project, user_path, "    private static TenantId requireTenantId(TenantId tenantId) {", f'''    public void changePreferredLanguage(String language) {{
        String validated = requirePreferredLanguage(language);
        if (!validated.equals(this.preferredLanguage)) {{
            this.preferredLanguage = validated;
            this.updatedTime = LocalDateTime.now();
        }}
    }}

    private static String requirePreferredLanguage(String language) {{
        if (language == null || language.isBlank() || language.length() > 10) {{
            throw new {PACKAGE}.domain.exception.InvalidValueException("Language must contain 1 to 10 characters");
        }}
        return language;
    }}

    private static TenantId requireTenantId(TenantId tenantId) {{''')
    _replace(project, _java("infra/persistence", "infra/persistence/mysql/po/UserPO.java"),
             "    private String realName;", "    private String realName;\n\n    @TableField(\"preferred_language\")\n    private String preferredLanguage = \"en\";")
    for module, relative in [("application", "application/vo/UserVO.java"), ("api", "api/dto/response/UserResponse.java")]:
        _replace(project, _java(module, relative), "    private String realName;", "    private String realName;\n    private String preferredLanguage;")
    mapper = "infra/persistence/src/main/resources/mapper/UserMapper.xml"
    _replace(project, mapper, '        <result column="real_name" property="realName" />',
             '        <result column="real_name" property="realName" />\n        <result column="preferred_language" property="preferredLanguage" />')
    _replace(project, mapper, "id, tenant_id, username, email, phone_number, password_hash, real_name, status,",
             "id, tenant_id, username, email, phone_number, password_hash, real_name, preferred_language, status,")
