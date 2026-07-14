package ${package}.application.service.impl;

import ${package}.api.context.AuthenticatedCaller;
import ${package}.api.dto.request.QueryRequest;
import ${package}.api.dto.request.UserCreateRequest;
import ${package}.api.dto.request.UserQueryRequest;
import ${package}.application.service.template.CommandServiceTemplate;
import ${package}.application.service.template.QueryServiceTemplate;
import ${package}.application.security.CallerGuard;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
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
    @Mock
    private PlatformTransactionManager transactionManager;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(
                        transactionManager.getTransaction(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new SimpleTransactionStatus());
        service = new UserServiceImpl(
                userRepository,
                userDomainService,
                domainEventPublisher,
                new CommandServiceTemplate("test-app", transactionManager),
                new QueryServiceTemplate("test-app"),
                userCacheService,
                userFactory,
                afterCommitExecutor,
                new CallerGuard());
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

    @Test
    void eventFailureCannotSkipIndependentCacheInvalidation() {
        TenantId tenantId = new TenantId(99L);
        UserId userId = new UserId(10L);
        User user = activeUser(tenantId, userId);
        when(userRepository.findById(tenantId, userId)).thenReturn(Optional.of(user));
        when(userRepository.save(tenantId, user)).thenReturn(user);

        Result<Void> result = service.updateUserStatus(caller("users:write"), 10L, "LOCKED");

        assertTrue(result.isSuccess());
        var callbacks = org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(afterCommitExecutor, org.mockito.Mockito.times(2)).execute(callbacks.capture());
        List<Runnable> actions = callbacks.getAllValues();
        doThrow(new IllegalStateException("publisher unavailable"))
                .when(domainEventPublisher).publishAll(anyList());
        assertThrows(IllegalStateException.class, actions.get(0)::run);
        actions.get(1).run();
        verify(userCacheService).invalidateUser(tenantId, userId);
    }

    private AuthenticatedCaller caller(String authority) {
        return new AuthenticatedCaller(7L, 99L, Set.of(authority));
    }

    private User deletedUser(TenantId tenantId, UserId userId) {
        return User.reconstitute(new User.UserSnapshot(
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
                LocalDateTime.now()));
    }

    private User adminUser(TenantId tenantId, UserId userId) {
        return User.reconstitute(new User.UserSnapshot(
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
                LocalDateTime.now()));
    }

    private User activeUser(TenantId tenantId, UserId userId) {
        return User.reconstitute(new User.UserSnapshot(
                userId,
                new Username("active_user"),
                new Email("active@example.com"),
                null,
                PasswordHash.fromTrustedHash("password-hash"),
                "Active User",
                UserStatus.ACTIVE,
                tenantId,
                null,
                false,
                false,
                1L,
                LocalDateTime.now(),
                LocalDateTime.now()));
    }

    private void assertFailure(Result<?> result, String expectedSpecificCode) {
        assertFalse(result.isSuccess());
        assertEquals(expectedSpecificCode,
                result.getErrorContext().fetchRootError().getErrorCode().getErrorSpecific());
    }
}
