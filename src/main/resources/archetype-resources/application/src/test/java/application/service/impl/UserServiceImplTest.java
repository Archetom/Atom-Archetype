package ${package}.application.service.impl;

import ${package}.api.context.AuthenticatedCaller;
import ${package}.api.dto.request.QueryRequest;
import ${package}.api.dto.request.UserCreateRequest;
import ${package}.api.dto.request.UserQueryRequest;
import ${package}.application.service.template.CommandServiceTemplate;
import ${package}.application.service.template.QueryServiceTemplate;
import ${package}.application.transaction.AfterCommitExecutor;
import ${package}.domain.entity.User;
import ${package}.domain.event.DomainEventPublisher;
import ${package}.domain.factory.UserFactory;
import ${package}.domain.model.UserStatus;
import ${package}.domain.repository.UserRepository;
import ${package}.domain.service.UserDomainService;
import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.PasswordHash;
import ${package}.domain.valueobject.TenantId;
import ${package}.domain.valueobject.UserId;
import ${package}.domain.valueobject.Username;
import io.github.archetom.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserDomainService userDomainService;
    @Mock
    private DomainEventPublisher domainEventPublisher;
    @Mock
    private UserCacheService userCacheService;
    @Mock
    private UserFactory userFactory;
    @Mock
    private AfterCommitExecutor afterCommitExecutor;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(
                userRepository,
                userDomainService,
                domainEventPublisher,
                new CommandServiceTemplate("test-app"),
                new QueryServiceTemplate("test-app"),
                userCacheService,
                userFactory,
                afterCommitExecutor);
    }

    @Test
    void statusUpdateCannotBypassDeleteAuthority() {
        Result<Void> result = service.updateUserStatus(caller("users:write"), 10L, "DELETED");

        assertFailure(result, "002");
        verifyNoInteractions(userRepository);
    }

    @Test
    void deleteRequiresDeleteAuthority() {
        Result<Void> result = service.deleteUser(caller("users:write"), 10L);

        assertFailure(result, "103");
        verifyNoInteractions(userRepository);
    }

    @Test
    void rejectsUnboundedPageSizeBeforeRepositoryAccess() {
        UserQueryRequest request = new UserQueryRequest();
        request.setSize(QueryRequest.MAX_SIZE + 1);

        Result<?> result = service.queryUsers(caller("users:read"), request);

        assertFailure(result, "101");
        verifyNoInteractions(userRepository);
    }

    @Test
    void invalidDomainValueBecomesParameterError() {
        UserCreateRequest request = new UserCreateRequest()
                .setUsername("alice")
                .setEmail("alice@localhost")
                .setPassword("long-enough-password");
        when(userFactory.createStandardUser(
                new TenantId(99L), "alice", "alice@localhost",
                "long-enough-password", null))
                .thenThrow(new IllegalArgumentException("invalid email"));

        Result<?> result = service.createUser(caller("users:write"), request);

        assertFailure(result, "101");
        verifyNoInteractions(userRepository);
    }

    @Test
    void deletedUserIsNotVisibleOrMutable() {
        TenantId tenantId = new TenantId(99L);
        UserId userId = new UserId(10L);
        when(userRepository.findById(tenantId, userId)).thenReturn(Optional.of(deletedUser(tenantId, userId)));

        Result<?> getResult = service.getUserById(caller("users:read"), 10L);
        Result<?> updateResult = service.updateUserStatus(caller("users:write"), 10L, "ACTIVE");
        Result<?> deleteResult = service.deleteUser(caller("users:delete"), 10L);

        assertFailure(getResult, "300");
        assertFailure(updateResult, "300");
        assertFailure(deleteResult, "300");
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void administratorCannotBeDeleted() {
        TenantId tenantId = new TenantId(99L);
        UserId userId = new UserId(10L);
        when(userRepository.findById(tenantId, userId)).thenReturn(Optional.of(adminUser(tenantId, userId)));
        when(userDomainService.canDeleteUser(org.mockito.ArgumentMatchers.any())).thenReturn(false);

        Result<?> result = service.deleteUser(caller("users:delete"), 10L);

        assertFailure(result, "002");
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private AuthenticatedCaller caller(String authority) {
        return new AuthenticatedCaller(7L, 99L, Set.of(authority));
    }

    private User deletedUser(TenantId tenantId, UserId userId) {
        return User.reconstitute(
                userId,
                new Username("deleted_user"),
                new Email("deleted@example.com"),
                null,
                PasswordHash.fromTrustedHash("password-hash"),
                "Deleted User",
                UserStatus.DELETED,
                tenantId,
                null,
                false,
                false,
                1L,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private User adminUser(TenantId tenantId, UserId userId) {
        return User.reconstitute(
                userId,
                new Username("admin_user"),
                new Email("admin@example.com"),
                null,
                PasswordHash.fromTrustedHash("password-hash"),
                "Admin User",
                UserStatus.ACTIVE,
                tenantId,
                null,
                false,
                true,
                1L,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private void assertFailure(Result<?> result, String expectedSpecificCode) {
        assertFalse(result.isSuccess());
        assertEquals(expectedSpecificCode,
                result.getErrorContext().fetchRootError().getErrorCode().getErrorSpecific());
    }
}
